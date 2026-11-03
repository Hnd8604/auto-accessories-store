# CI/CD — từng job làm gì

Repo có **hai** workflow độc lập, không job nào của workflow này `needs` job của
workflow kia:

| Workflow | File | Nhiệm vụ |
|---|---|---|
| **CI** | [.github/workflows/ci.yml](../.github/workflows/ci.yml) | Chạy test, build thử. Không đẩy gì ra ngoài repo |
| **CD** | [.github/workflows/cd.yml](../.github/workflows/cd.yml) | Build image đẩy lên `ghcr.io`, và (khi được yêu cầu) deploy lên VPS |

GitHub gộp check của cả hai vào chung một danh sách trên cùng commit — bảng
"sự kiện nào chạy gì" nằm ở [DEPLOY.md § Sự kiện nào chạy gì](DEPLOY.md#sự-kiện-nào-chạy-gì).
Tài liệu này giải thích **bên trong mỗi job**.

---

## CI — 4 job

```
changes ──┬─> backend  ──┬─> docker-build   (chỉ ở PR)
          └─> frontend ──┘
```

### `changes` — lọc theo đường dẫn

Job duy nhất luôn chạy. Dùng [dorny/paths-filter](https://github.com/dorny/paths-filter)
để xem commit/PR đụng vào phần nào, xuất ra 2 output `backend` / `frontend`:

| Output | `true` khi có file đổi trong |
|---|---|
| `backend` | `backend/**` hoặc `.github/workflows/ci.yml` |
| `frontend` | `frontend/**` hoặc `.github/workflows/ci.yml` |

Sửa mỗi frontend thì job `backend` bị skip → tiết kiệm ~1 phút và không phải
khởi động container Postgres vô ích. `ci.yml` nằm trong **cả hai** filter, nên
sửa chính workflow là chạy lại toàn bộ để kiểm chứng.

> Job này khai `permissions: pull-requests: read` vì với sự kiện
> `pull_request`, paths-filter gọi API để lấy danh sách file của PR.

### `backend` — test + đóng gói Spring Boot

Chạy khi `changes.outputs.backend == 'true'`. JDK 21 (temurin), cache `~/.m2`
theo `pom.xml`.

Điểm đáng chú ý: job này **có service container Postgres 16** (cùng version với
production), dù phần lớn unit test dùng Mockito và không cần database. Postgres
tồn tại chỉ để bật `SchemaMigrationTest` — biến `TEST_DB_URL` được truyền vào
mới kích hoạt test đó:

```
TEST_DB_URL=jdbc:postgresql://localhost:5432/storetest
```

Test đó chạy Flyway trên một DB rỗng rồi bắt Hibernate `validate`, nên **entity
lệch với migration là fail ngay ở CI** thay vì làm backend chết lúc khởi động
trên VPS (xem [SchemaMigrationTest](../backend/src/test/java/app/store/SchemaMigrationTest.java)).

Các bước, theo thứ tự:

| Bước | Lệnh / hành động | Ghi chú |
|---|---|---|
| Test | `./mvnw -B test` | unit test + schema migration test |
| Package | `./mvnw -B package -DskipTests` | không chạy lại test đã pass |
| Upload báo cáo | artifact `backend-surefire-reports` | `if: always()` — có cả khi test fail, để tải về đọc |
| Upload jar | artifact `backend-jar` | giữ 7 ngày |

Jar này **không** được CD dùng lại — CD build image từ `Dockerfile` riêng.
Artifact ở đây chỉ để tải về debug.

### `frontend` — lint, type check, build Vite

Chạy khi `changes.outputs.frontend == 'true'`. Node 22 (khớp `node:22-alpine`
trong [frontend/Dockerfile](../frontend/Dockerfile)), cache npm theo
`frontend/package-lock.json`, cài bằng `npm ci`.

| Bước | Chặn merge? | Vì sao |
|---|---|---|
| `npm run lint` | ❌ `continue-on-error` | Code hiện còn ~66 lỗi eslint (chủ yếu `no-explicit-any`) |
| `npx tsc --noEmit -p tsconfig.app.json` | ❌ `continue-on-error` | Còn ~17 lỗi type |
| `npm run build` | ✅ **có** | Build hỏng là chặn |
| Upload `frontend-dist` | — | artifact giữ 7 ngày |

Type check phải chạy tay bằng `tsc` vì Vite dùng `plugin-react-swc` — `npm run
build` **không** type-check. Hai bước advisory đang để cảnh báo; dọn sạch lỗi
rồi thì bỏ `continue-on-error` để chúng chặn hồi quy.

Bước build truyền `VITE_API_BASE_URL=/api/v1` giống hệt lúc CD build image, để
CI không xanh với một cấu hình khác với bản thật.

### `docker-build` — build thử image, **chỉ ở PR**

```yaml
if: always() && github.event_name == 'pull_request' && !contains(needs.*.result, 'failure')
```

Ba mảnh của điều kiện này:

- `github.event_name == 'pull_request'` — push vào `main` đã có CD build & push
  image thật, build lại ở CI là thừa. **Đây là lý do check này hiện `skipped`
  khi push `main`.**
- `always()` — cần để job vẫn chạy khi `backend`/`frontend` bị `changes` skip.
  Không có `always()`, một `needs` bị skip là kéo theo job này skip luôn.
- `!contains(needs.*.result, 'failure')` — nhưng nếu có job **fail** thì bỏ qua,
  không tốn thêm runner.

Job build image bằng buildx với `push: false` (không đăng nhập registry, không
đẩy gì lên), cache qua GitHub Actions cache. Mỗi image chỉ build khi phần tương
ứng có thay đổi. Mục đích duy nhất: bắt lỗi `Dockerfile` **trước khi** merge,
thay vì để CD gãy sau khi đã vào `main`.

### Concurrency

```yaml
group: ci-${{ github.workflow }}-${{ github.ref }}
cancel-in-progress: true
```

Push liên tiếp vào cùng branch/PR sẽ **huỷ** lần chạy cũ — chỉ commit mới nhất
mới đáng quan tâm.

---

## CD — 2 job

```
build-push (matrix: backend, frontend) ──> deploy   (chỉ khi tag v* / deploy=true)
```

### `build-push` — build & đẩy image lên ghcr.io

Một `strategy.matrix` 2 dòng nên hiện ra **2 check riêng**: `build-push
(backend, ./backend)` và `build-push (frontend, ./frontend)`, chạy song song.

Khác với CI, job này **không** lọc theo paths-filter: mọi lần push `main` đều
build lại cả hai image, để một tag image luôn tương ứng với đúng một commit của
toàn repo — không có chuyện `backend:abc123` tồn tại mà `frontend:abc123` thì
không.

**Chốt chặn biến bắt buộc.** Trước khi build frontend, job fail ngay nếu chưa
đặt repository variable `GOOGLE_CLIENT_ID`. Thiếu biến này thì build vẫn xanh,
image vẫn push, chỉ tới khi người dùng bấm "Đăng nhập Google" mới lỗi — nên
chặn tại đây rẻ hơn nhiều.

**Build args chỉ frontend mới có.** Biến `VITE_*` bị nhúng cứng vào bundle lúc
build, không đọc được lúc chạy container:

```
VITE_API_BASE_URL=/api/v1
VITE_GOOGLE_CLIENT_ID=${{ vars.GOOGLE_CLIENT_ID }}
```

Cố tình **không** có `VITE_GOOGLE_REDIRECT_URI`: frontend suy ra
`${window.location.origin}/auth/google/callback` lúc chạy, nên bundle không biết
gì về tên miền → đổi domain không cần build lại frontend, và cùng một image
chạy được cho cả staging lẫn production.

> Khoá trong matrix đặt tên `build_args` (gạch dưới) chứ không phải
> `build-args`: trong expression của Actions, `matrix.build-args` bị parse
> thành **phép trừ**.

**Đăng nhập registry** bằng `GITHUB_TOKEN` có sẵn (`permissions: packages:
write`) — không cần tạo secret riêng.

**Tag image** do `docker/metadata-action` sinh:

| Quy tắc | Ra tag | Khi nào |
|---|---|---|
| `type=raw,value=latest,enable={{is_default_branch}}` | `latest` | **chỉ** khi push `main` |
| `type=sha,prefix=,format=short` | `a1b2c3d` | mọi lần |
| `type=semver,pattern={{version}}` | `1.2.3` | push tag `v1.2.3` |
| `type=semver,pattern={{major}}.{{minor}}` | `1.2` | push tag `v1.2.3` |

Hệ quả quan trọng: **push tag `v1.2.3` không tạo ra `latest`**. Đó là lý do job
`deploy` phải tự chốt tag thay vì để server đọc `IMAGE_TAG=latest`.

Cache buildx dùng GitHub Actions cache, tách scope theo `matrix.name` để backend
và frontend không ghi đè cache của nhau.

### `deploy` — SSH lên VPS

```yaml
needs: build-push
if: startsWith(github.ref, 'refs/tags/v') || inputs.deploy
environment: production
```

Chỉ chạy khi push tag `v*` hoặc bấm **Run workflow** với `deploy = true` — merge
vào `main` thì check này `skipped`. `environment: production` cho phép bật
*required reviewers* trong Settings để deploy phải có người duyệt.

`needs: build-push` đảm bảo image đã nằm trên registry trước khi server `pull`.

Các bước:

1. **Xác định image tag.** Push tag → `${GITHUB_REF_NAME#v}` (bỏ tiền tố `v`,
   khớp `type=semver`). Run workflow từ `main` → `${GITHUB_SHA::7}` (khớp
   `type=sha`). Tag được **chốt trong workflow rồi truyền xuống**, không lấy từ
   `IMAGE_TAG` trong `.env.prod` — nếu lấy từ đó, deploy một tag sẽ dựng lên bản
   `main` gần nhất chứ không phải bản vừa tag.

2. **Deploy over SSH** ([appleboy/ssh-action](https://github.com/appleboy/ssh-action),
   `script_stop: true` — dừng ngay khi một lệnh fail). Trên server:

   | Việc | Vì sao |
   |---|---|
   | In tag cũ → tag mới | Đọc log là biết vừa nhảy từ đâu sang đâu |
   | `sed` ghi `IMAGE_TAG` mới vào `.env.prod` | Không ghi thì lần sau chạy tay `docker compose up -d` sẽ đọc lại tag cũ và **âm thầm hạ cấp** |
   | `docker login ghcr.io` bằng `GITHUB_TOKEN` | Image private, cần `permissions: packages: read` |
   | `pull` → `up -d --remove-orphans --wait --wait-timeout 180` | `--wait` chờ healthcheck từng service pass rồi mới trả về, thay vì trả về ngay lúc container vừa start |
   | `docker image prune --filter "until=168h"` | Chỉ xoá image dangling quá 1 tuần — giữ vài bản gần đây để rollback ngay được |
   | `docker compose ps` | Chụp lại trạng thái cuối vào log |

3. **Health check.** Gọi `https://$APP_DOMAIN/api/v1/actuator/health` tối đa 20
   lần, cách nhau 10 giây, chờ `"status":"UP"`. `APP_DOMAIN` lấy từ repository
   variable và **chỉ** dùng cho bước này — domain thật của ứng dụng do
   `APP_DOMAIN` trong `.env.prod` trên VPS quyết định. Đặt lệch nhau thì chỉ
   health check báo đỏ, ứng dụng vẫn chạy bình thường.

### Concurrency

```yaml
group: cd-${{ github.ref }}
cancel-in-progress: false
```

Ngược với CI: **không huỷ** lần chạy đang dở. Cắt ngang một deploy giữa chừng có
thể để lại cụm container ở trạng thái nửa vời.

---

## Bí mật & biến cần khai

Ở **Settings → Secrets and variables → Actions**. Chi tiết xem
[DEPLOY.md § 7](DEPLOY.md#7-deploy-tự-động-qua-github-actions).

| Tên | Loại | Job dùng |
|---|---|---|
| `GOOGLE_CLIENT_ID` | Variable | `build-push` (frontend) |
| `APP_DOMAIN` | Variable | `deploy` (health check) |
| `DEPLOY_PATH` | Variable | `deploy` (mặc định `/opt/auto_accessories_store`) |
| `SSH_HOST`, `SSH_USER`, `SSH_KEY`, `SSH_PORT` | Secret | `deploy` |

`GITHUB_TOKEN` là token có sẵn, không cần khai.

---

## Debug khi CI/CD đỏ

| Triệu chứng | Xem ở đâu |
|---|---|
| `backend` fail ở bước test | Tải artifact `backend-surefire-reports` của run đó |
| Backend fail với lỗi Flyway/Hibernate validate | `SchemaMigrationTest` — entity và migration đang lệch nhau |
| `frontend` fail | Chỉ có thể ở bước `npm run build`; lint và tsc là advisory |
| `build-push (frontend)` fail ngay bước đầu | Chưa đặt repository variable `GOOGLE_CLIENT_ID` |
| `deploy` fail ở bước SSH | Sai `SSH_*`, sai `DEPLOY_PATH`, hoặc thiếu `.env.prod` trên server |
| `deploy` fail ở Health check | Container đã lên nhưng backend chưa `UP` — SSH vào chạy `docker compose logs -f backend` |
| Job cần chạy mà lại `skipped` | Đọc lại [bảng sự kiện](DEPLOY.md#sự-kiện-nào-chạy-gì) — hầu hết là do điều kiện `if`, không phải lỗi |

Chạy lại một run: **Actions → chọn run → Re-run failed jobs**. Riêng `deploy`
không re-run được từ một run đã skip — phải push tag mới hoặc bấm **Run
workflow** với `deploy = true`.

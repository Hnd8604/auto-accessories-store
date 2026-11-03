package app.store.seed.dev;

import java.util.List;

/**
 * Dữ liệu mẫu (tiếng Việt) cho {@link DevDataSeeder}. Chỉ chứa dữ liệu tĩnh,
 * không có logic — muốn thêm/sửa sản phẩm, bài viết... thì sửa ở đây.
 */
final class DevSeedData {

    private DevSeedData() {
    }

    // ------------------------------------------------------------------
    // Kiểu dữ liệu
    // ------------------------------------------------------------------

    record BrandSeed(String key, String name, String description) {
    }

    /** {@code color} là màu nền (hex, không có #) cho ảnh placeholder của danh mục. */
    record CategorySeed(String key, String name, String description, String color) {
    }

    record ProductSeed(String category, String brand, String name, long price, String highlight) {
    }

    record CustomerSeed(String username, String fullName, String phone) {
    }

    record PostSeed(String category, String title, String shortDescription, String content, boolean published) {
    }

    record ServiceSeed(String name, String shortDescription, String fullDescription, List<String> features) {
    }

    /** {@code redirectUrl} dạng {@code product:<tên sản phẩm>} sẽ được đổi thành /products/<slug>. */
    record BannerSeed(String title, String subtitle, String redirectUrl, String buttonText, String color) {
    }

    record ChatSeed(String guestName, String status, List<String[]> messages) {
    }

    // ------------------------------------------------------------------
    // Danh mục & thương hiệu
    // ------------------------------------------------------------------

    static final List<BrandSeed> BRANDS = List.of(
            new BrandSeed("70mai", "70mai", "Thương hiệu camera hành trình thuộc hệ sinh thái Xiaomi, nổi bật với thiết kế tối giản và điều khiển bằng giọng nói."),
            new BrandSeed("vietmap", "Vietmap", "Thương hiệu Việt Nam chuyên camera hành trình, dẫn đường và cảnh báo giao thông cho người Việt."),
            new BrandSeed("hikvision", "Hikvision", "Nhà sản xuất thiết bị an ninh hàng đầu thế giới, dòng camera ô tô bền bỉ, hình ảnh sắc nét."),
            new BrandSeed("xiaomi", "Xiaomi", "Phụ kiện công nghệ cho ô tô với mức giá hợp lý, chất lượng ổn định."),
            new BrandSeed("zestech", "Zestech", "Thương hiệu màn hình Android ô tô phổ biến tại Việt Nam, tích hợp camera 360 và trợ lý giọng nói."),
            new BrandSeed("teyes", "Teyes", "Màn hình Android ô tô cao cấp, giao diện mượt, hỗ trợ CarPlay và Android Auto không dây."),
            new BrandSeed("gotech", "Gotech", "Giải pháp màn hình thông minh và camera 360 độ cho nhiều dòng xe phổ thông."),
            new BrandSeed("steelmate", "Steelmate", "Chuyên cảm biến áp suất lốp, cảm biến lùi và các thiết bị an toàn ô tô."),
            new BrandSeed("philips", "Philips", "Bóng đèn ô tô halogen, LED chính hãng với độ sáng cao và tuổi thọ dài."),
            new BrandSeed("osram", "Osram", "Thương hiệu chiếu sáng đến từ Đức, bóng đèn ô tô đạt chuẩn châu Âu."),
            new BrandSeed("pioneer", "Pioneer", "Thương hiệu âm thanh xe hơi Nhật Bản với loa, sub và đầu phát chất lượng."),
            new BrandSeed("jbl", "JBL", "Loa ô tô JBL cho âm thanh sống động, dải trầm mạnh mẽ."),
            new BrandSeed("baseus", "Baseus", "Phụ kiện sạc, giá đỡ điện thoại và đồ tiện ích cho ô tô với thiết kế hiện đại."),
            new BrandSeed("anker", "Anker", "Sạc ô tô và phụ kiện điện tử với công nghệ sạc nhanh an toàn."),
            new BrandSeed("ugreen", "Ugreen", "Cáp, sạc và phụ kiện kết nối chất lượng cao, giá tốt."),
            new BrandSeed("3m", "3M", "Phim cách nhiệt, sản phẩm chăm sóc và bảo vệ bề mặt xe từ Mỹ."),
            new BrandSeed("sonax", "Sonax", "Hóa chất chăm sóc xe chuyên nghiệp đến từ Đức."),
            new BrandSeed("meguiars", "Meguiar's", "Thương hiệu chăm sóc xe hơn 100 năm tuổi, nổi tiếng với dòng wax và dung dịch rửa xe."),
            new BrandSeed("maxpider", "Maxpider", "Thảm lót sàn ô tô thiết kế riêng theo từng dòng xe, chống nước, dễ vệ sinh."),
            new BrandSeed("bosch", "Bosch", "Phụ tùng ô tô Đức: gạt mưa, còi, bugi... chất lượng OEM."),
            new BrandSeed("michelin", "Michelin", "Ngoài lốp xe, Michelin còn có gạt mưa, bơm lốp và dụng cụ cứu hộ."),
            new BrandSeed("llumar", "Llumar", "Phim cách nhiệt cao cấp từ Mỹ, cản nhiệt và chống tia UV hiệu quả.")
    );

    static final List<CategorySeed> CATEGORIES = List.of(
            new CategorySeed("camera", "Camera hành trình", "Camera ghi hình hành trình trước/sau, hỗ trợ GPS, cảnh báo ADAS và giám sát đỗ xe.", "0f172a"),
            new CategorySeed("android", "Màn hình Android ô tô", "Màn hình thông minh thay thế màn zin, tích hợp dẫn đường, giải trí, CarPlay.", "1e3a8a"),
            new CategorySeed("sensor", "Cảm biến & camera lùi", "Cảm biến áp suất lốp, cảm biến lùi, camera lùi và camera 360 độ.", "134e4a"),
            new CategorySeed("light", "Đèn ô tô", "Bóng đèn pha, đèn sương mù, đèn LED nội thất cho ô tô.", "713f12"),
            new CategorySeed("audio", "Âm thanh ô tô", "Loa cánh, loa sub, amply giúp nâng cấp hệ thống âm thanh xe.", "581c87"),
            new CategorySeed("electronic", "Phụ kiện điện tử & sạc", "Tẩu sạc, giá đỡ điện thoại, sạc không dây và các thiết bị tiện ích.", "374151"),
            new CategorySeed("care", "Chăm sóc & vệ sinh xe", "Dung dịch rửa xe, wax, phục hồi nhựa nhám, khử mùi nội thất.", "065f46"),
            new CategorySeed("interior", "Thảm lót sàn & nội thất", "Thảm lót sàn, thảm cốp, gối tựa đầu và phụ kiện nội thất.", "7c2d12"),
            new CategorySeed("wiper", "Gạt mưa & phụ tùng", "Gạt mưa, còi xe và các phụ tùng thay thế nhanh.", "1e293b"),
            new CategorySeed("rescue", "Dụng cụ & cứu hộ", "Bơm lốp mini, kích nâng, bộ kích nổ bình và dụng cụ khẩn cấp.", "7f1d1d"),
            new CategorySeed("film", "Phim cách nhiệt", "Phim cách nhiệt kính lái, kính sườn, chống tia UV và chói nắng.", "164e63")
    );

    // ------------------------------------------------------------------
    // Sản phẩm (~85)
    // ------------------------------------------------------------------

    static final List<ProductSeed> PRODUCTS = List.of(
            // Camera hành trình
            new ProductSeed("camera", "70mai", "Camera hành trình 70mai A810 4K", 3_290_000, "Ghi hình 4K HDR cảm biến Sony STARVIS 2, GPS tích hợp, hỗ trợ camera sau."),
            new ProductSeed("camera", "70mai", "Camera hành trình 70mai A500S kèm cam sau", 2_190_000, "Ghi hình 2.7K trước + 1080p sau, ADAS cảnh báo lệch làn, giám sát đỗ xe 24h."),
            new ProductSeed("camera", "70mai", "Camera hành trình 70mai M500 32GB", 1_590_000, "Thiết kế nhỏ gọn ẩn sau gương, eMMC 32GB tích hợp không cần thẻ nhớ."),
            new ProductSeed("camera", "70mai", "Camera hành trình 70mai Dash Cam Lite 2", 890_000, "Độ phân giải 1080p, góc rộng 130°, điều khiển qua ứng dụng điện thoại."),
            new ProductSeed("camera", "vietmap", "Camera hành trình Vietmap C61 Pro", 3_990_000, "Ghi hình 4K, cảnh báo giao thông bằng giọng nói, cập nhật dữ liệu camera phạt nguội."),
            new ProductSeed("camera", "vietmap", "Camera hành trình Vietmap KC01 Pro", 2_590_000, "Camera kép trước sau, cảnh báo tốc độ giới hạn từng đoạn đường Việt Nam."),
            new ProductSeed("camera", "vietmap", "Camera gương Vietmap iDVR P2", 4_490_000, "Màn hình gương 10 inch, dẫn đường Vietmap S1, camera lùi Full HD."),
            new ProductSeed("camera", "hikvision", "Camera hành trình Hikvision C6 Pro", 2_890_000, "Ghi hình 1600p, kết nối 4G xem trực tuyến từ xa, định vị GPS."),
            new ProductSeed("camera", "hikvision", "Camera hành trình Hikvision F6 Pro", 1_490_000, "Cảm biến ảnh lớn, quay đêm rõ nét, hỗ trợ thẻ nhớ đến 128GB."),
            new ProductSeed("camera", "xiaomi", "Camera hành trình Xiaomi Mijia 2K", 1_290_000, "Độ phân giải 2K, màn hình 3 inch, chống rung điện tử."),

            // Màn hình Android
            new ProductSeed("android", "zestech", "Màn hình Android Zestech Z800 Pro+", 9_900_000, "Chip 8 nhân, RAM 4GB, tích hợp camera 360 và trợ lý ảo tiếng Việt."),
            new ProductSeed("android", "zestech", "Màn hình Android Zestech ZT360", 12_500_000, "Liền camera 360 độ, màn QLED 2K, điều khiển giọng nói không chạm."),
            new ProductSeed("android", "zestech", "Màn hình Android Zestech S500", 6_490_000, "Màn hình 10 inch, RAM 2GB, hỗ trợ CarPlay có dây."),
            new ProductSeed("android", "teyes", "Màn hình Android Teyes CC3 2K", 14_900_000, "Màn QLED 2K, RAM 6GB, DSP 48 kênh, CarPlay/Android Auto không dây."),
            new ProductSeed("android", "teyes", "Màn hình Android Teyes SPRO Plus", 8_900_000, "RAM 4GB, sim 4G, giao diện TEYES mượt, hỗ trợ camera 360."),
            new ProductSeed("android", "gotech", "Màn hình Android Gotech GT10 Pro", 7_990_000, "Màn hình IPS 10 inch, RAM 3GB, tích hợp bản đồ Vietmap S1."),
            new ProductSeed("android", "gotech", "Màn hình Android Gotech GT360 Plus", 11_490_000, "Camera 360 độ AHD, chip 8 nhân, hỗ trợ sim 4G."),
            new ProductSeed("android", "vietmap", "Màn hình Android Vietmap W810", 10_990_000, "Dẫn đường Vietmap Live, cảnh báo giao thông, kết nối camera hành trình."),

            // Cảm biến & camera lùi
            new ProductSeed("sensor", "steelmate", "Cảm biến áp suất lốp Steelmate TP-S1", 1_850_000, "Van ngoài, màn hình năng lượng mặt trời, cảnh báo áp suất và nhiệt độ."),
            new ProductSeed("sensor", "steelmate", "Cảm biến áp suất lốp Steelmate E3 van trong", 2_650_000, "Cảm biến gắn trong lốp, hiển thị trên màn hình Android qua USB."),
            new ProductSeed("sensor", "steelmate", "Cảm biến lùi Steelmate 4 mắt", 950_000, "4 mắt cảm biến, còi báo khoảng cách, lắp đặt cho mọi dòng xe."),
            new ProductSeed("sensor", "vietmap", "Camera lùi Vietmap AHD 1080p", 790_000, "Camera lùi độ phân giải cao, góc rộng 170°, chống nước IP68."),
            new ProductSeed("sensor", "zestech", "Camera 360 độ Zestech DX360", 6_900_000, "4 camera AHD ghép hình 3D, ghi hình 4 kênh khi lái xe."),
            new ProductSeed("sensor", "gotech", "Camera 360 độ Gotech DCT Pro", 5_490_000, "Góc nhìn 3D xoay 360°, hỗ trợ đỗ xe trong không gian hẹp."),
            new ProductSeed("sensor", "xiaomi", "Cảm biến áp suất lốp 70mai TPMS Lite", 1_190_000, "Kết nối điện thoại qua Bluetooth, pin dùng 2 năm."),

            // Đèn ô tô
            new ProductSeed("light", "philips", "Bóng đèn pha LED Philips Ultinon Pro6000 H4", 2_590_000, "Sáng hơn 230%, nhiệt màu 5800K, tuổi thọ lên đến 5.000 giờ."),
            new ProductSeed("light", "philips", "Bóng đèn halogen Philips X-tremeVision H7", 490_000, "Tăng 150% độ sáng, chiếu xa hơn 45m so với bóng thường."),
            new ProductSeed("light", "philips", "Đèn LED nội thất Philips T10", 190_000, "Ánh sáng trắng 6000K, tiết kiệm điện, lắp đặt cắm là chạy."),
            new ProductSeed("light", "osram", "Bóng đèn LED Osram LEDriving HL Bright H11", 2_190_000, "Ánh sáng 6000K, tản nhiệt thông minh, chuẩn châu Âu."),
            new ProductSeed("light", "osram", "Bóng đèn halogen Osram Night Breaker 200 H4", 590_000, "Sáng hơn 200%, chùm sáng xa hơn 150m."),
            new ProductSeed("light", "osram", "Đèn sương mù LED Osram LEDriving FOG 101", 3_290_000, "Đèn sương mù LED kèm đèn ban ngày, chống nước IP67."),

            // Âm thanh
            new ProductSeed("audio", "pioneer", "Loa cánh Pioneer TS-A1670F 6.5 inch", 1_690_000, "Loa đồng trục 3 đường tiếng, công suất 320W, âm thanh trong trẻo."),
            new ProductSeed("audio", "pioneer", "Loa sub gầm ghế Pioneer TS-WX130DA", 4_290_000, "Sub điện tích hợp amply 160W, kích thước gọn đặt dưới ghế."),
            new ProductSeed("audio", "pioneer", "Amply 4 kênh Pioneer GM-D9705", 5_890_000, "Công suất tối đa 1600W, class D tiết kiệm năng lượng."),
            new ProductSeed("audio", "jbl", "Loa cánh JBL Stage3 627F", 1_490_000, "Loa đồng trục 6.5 inch, độ nhạy cao, phù hợp nâng cấp loa zin."),
            new ProductSeed("audio", "jbl", "Loa sub JBL BassPro SL2", 6_490_000, "Sub điện 8 inch tích hợp amply 125W, điều khiển bass từ xa."),
            new ProductSeed("audio", "jbl", "Bộ loa tách tần JBL Club 6500C", 3_990_000, "Loa tách tần 2 đường tiếng, treble lụa, âm trung chi tiết."),

            // Phụ kiện điện tử & sạc
            new ProductSeed("electronic", "baseus", "Tẩu sạc ô tô Baseus 65W 3 cổng", 450_000, "1 USB-C PD + 2 USB-A, sạc nhanh laptop và điện thoại."),
            new ProductSeed("electronic", "baseus", "Giá đỡ điện thoại Baseus kẹp cửa gió", 290_000, "Kẹp chắc chắn, xoay 360°, một tay thao tác."),
            new ProductSeed("electronic", "baseus", "Giá đỡ kiêm sạc không dây Baseus 15W", 690_000, "Tự động kẹp khi đặt điện thoại, sạc không dây chuẩn Qi."),
            new ProductSeed("electronic", "baseus", "Máy lọc không khí ô tô Baseus", 990_000, "Lọc bụi mịn PM2.5, khử mùi, tạo ion âm."),
            new ProductSeed("electronic", "anker", "Tẩu sạc ô tô Anker PowerDrive III 2 cổng", 390_000, "Công nghệ PowerIQ 3.0, sạc nhanh 36W."),
            new ProductSeed("electronic", "anker", "Tẩu sạc Anker 535 67W", 790_000, "Sạc nhanh 67W kèm cáp USB-C, đủ sạc MacBook Air."),
            new ProductSeed("electronic", "ugreen", "Tẩu sạc ô tô Ugreen 130W", 650_000, "3 cổng sạc, công suất tổng 130W, vỏ hợp kim nhôm."),
            new ProductSeed("electronic", "ugreen", "Bộ thu Bluetooth ô tô Ugreen 5.0", 320_000, "Biến loa xe thành loa Bluetooth, hỗ trợ đàm thoại rảnh tay."),
            new ProductSeed("electronic", "xiaomi", "Tẩu sạc nhanh Xiaomi 100W", 420_000, "USB-C + USB-A, sạc nhanh 100W, đèn LED báo nguồn."),
            new ProductSeed("electronic", "70mai", "Giá đỡ sạc không dây 70mai Wireless Car Charger", 590_000, "Cảm biến hồng ngoại tự động kẹp mở, sạc 15W."),

            // Chăm sóc & vệ sinh
            new ProductSeed("care", "3m", "Dung dịch rửa xe 3M Car Wash Soap 1L", 220_000, "Tạo bọt dày, an toàn cho lớp sơn, không làm mất wax."),
            new ProductSeed("care", "3m", "Dung dịch làm sạch nội thất 3M", 180_000, "Làm sạch ghế da, nỉ, nhựa nội thất, khử mùi nhẹ."),
            new ProductSeed("care", "3m", "Kem đánh bóng xóa xước 3M", 260_000, "Xóa vết xước nhẹ, phục hồi độ bóng cho sơn xe."),
            new ProductSeed("care", "sonax", "Nước rửa kính Sonax Xtreme 3L", 250_000, "Làm sạch kính, chống bám côn trùng, không để lại vệt."),
            new ProductSeed("care", "sonax", "Dung dịch phục hồi nhựa nhám Sonax", 290_000, "Phục hồi nhựa đen ngoại thất, chống tia UV."),
            new ProductSeed("care", "sonax", "Xịt phủ bóng nhanh Sonax Xtreme Ceramic", 450_000, "Phủ ceramic dạng xịt, hiệu ứng lá sen, bóng đến 3 tháng."),
            new ProductSeed("care", "meguiars", "Nước rửa xe Meguiar's Gold Class 1.89L", 390_000, "Kết hợp rửa và dưỡng bóng, mùi thơm dễ chịu."),
            new ProductSeed("care", "meguiars", "Wax đánh bóng Meguiar's Ultimate Liquid Wax", 690_000, "Wax tổng hợp tạo độ bóng sâu, bảo vệ sơn lâu dài."),
            new ProductSeed("care", "meguiars", "Khăn microfiber Meguiar's Supreme (bộ 3)", 290_000, "Khăn siêu mịn, thấm hút tốt, không gây xước sơn."),
            new ProductSeed("care", "baseus", "Máy hút bụi cầm tay ô tô Baseus A3", 890_000, "Lực hút 15000Pa, pin sạc, nhỏ gọn để trong cốp."),

            // Thảm & nội thất
            new ProductSeed("interior", "maxpider", "Thảm lót sàn Maxpider Toyota Vios", 2_490_000, "Thiết kế theo khuôn xe Vios, chất liệu TPE không mùi, chống nước."),
            new ProductSeed("interior", "maxpider", "Thảm lót sàn Maxpider Mazda CX-5", 2_790_000, "Ôm sát sàn xe CX-5, gờ cao chống tràn nước."),
            new ProductSeed("interior", "maxpider", "Thảm lót sàn Maxpider Hyundai Accent", 2_390_000, "Chất liệu cao su TPE cao cấp, dễ vệ sinh."),
            new ProductSeed("interior", "maxpider", "Thảm lót sàn Maxpider Kia Seltos", 2_590_000, "Thiết kế 3D theo xe Seltos, bám chắc không xô lệch."),
            new ProductSeed("interior", "maxpider", "Thảm lót cốp Maxpider Honda CR-V", 1_290_000, "Thảm cốp chống trầy, chống nước, dễ tháo lắp."),
            new ProductSeed("interior", "maxpider", "Thảm lót sàn Maxpider Ford Ranger", 2_890_000, "Phù hợp xe bán tải Ranger, chịu mài mòn cao."),
            new ProductSeed("interior", "baseus", "Gối tựa đầu ô tô Baseus ComfortRide", 490_000, "Đệm memory foam, giảm mỏi cổ khi lái đường dài."),
            new ProductSeed("interior", "baseus", "Đệm lưng ô tô Baseus ComfortRide", 690_000, "Hỗ trợ cột sống thắt lưng, vải thoáng khí."),

            // Gạt mưa & phụ tùng
            new ProductSeed("wiper", "bosch", "Gạt mưa Bosch AeroTwin 24 inch", 450_000, "Gạt mưa không xương, áp lực đều, chạy êm không tiếng ồn."),
            new ProductSeed("wiper", "bosch", "Gạt mưa Bosch Clear Advantage 18 inch", 290_000, "Lưỡi cao su phủ graphite, bền bỉ với thời tiết nóng."),
            new ProductSeed("wiper", "bosch", "Còi sên Bosch EC6 (cặp)", 650_000, "Âm thanh to, rõ, chuẩn châu Âu, chống nước."),
            new ProductSeed("wiper", "michelin", "Gạt mưa Michelin Stealth Hybrid 22 inch", 390_000, "Thiết kế lai, khí động học, lắp vừa đa số xe."),
            new ProductSeed("wiper", "michelin", "Gạt mưa Michelin Radius 16 inch", 220_000, "Gạt mưa xương sắt giá tốt, cao su tự nhiên."),
            new ProductSeed("wiper", "osram", "Gạt mưa Osram Wiper Blade 20 inch", 260_000, "Lưỡi gạt mềm, gạt sạch không để lại vệt nước."),

            // Dụng cụ & cứu hộ
            new ProductSeed("rescue", "xiaomi", "Bơm lốp điện tử Xiaomi Mijia 1S", 990_000, "Bơm tự ngắt theo áp suất cài đặt, pin sạc, đèn LED."),
            new ProductSeed("rescue", "70mai", "Bơm lốp ô tô 70mai Midrive TP03", 1_190_000, "Bơm nhanh 0-36 PSI trong 5 phút, màn hình số."),
            new ProductSeed("rescue", "michelin", "Bơm lốp mini Michelin 12V", 850_000, "Cắm tẩu 12V, đồng hồ áp suất cơ, kèm đầu chuyển."),
            new ProductSeed("rescue", "baseus", "Bộ kích nổ bình Baseus 1000A", 1_690_000, "Kích nổ xe máy xăng đến 4.0L, kiêm sạc dự phòng."),
            new ProductSeed("rescue", "70mai", "Bộ kích nổ bình 70mai Jump Starter Max", 2_290_000, "Dòng khởi động 1000A, đèn pin LED, bảo vệ đảo cực."),
            new ProductSeed("rescue", "bosch", "Bộ dụng cụ sửa chữa ô tô Bosch 46 chi tiết", 1_290_000, "Cờ lê, tuýp, tua vít đầy đủ trong hộp nhựa gọn."),
            new ProductSeed("rescue", "michelin", "Tam giác cảnh báo phản quang Michelin", 190_000, "Gấp gọn, phản quang tốt, chân đế chống gió."),

            // Phim cách nhiệt
            new ProductSeed("film", "3m", "Phim cách nhiệt 3M Crystalline (xe 5 chỗ)", 12_900_000, "Cản 97% tia hồng ngoại, không cản sóng điện thoại, bảo hành 10 năm."),
            new ProductSeed("film", "3m", "Phim cách nhiệt 3M Ceramic IR (xe 5 chỗ)", 7_900_000, "Phim gốm cách nhiệt, độ trong cao, chống chói."),
            new ProductSeed("film", "3m", "Phim cách nhiệt 3M Ceramic IR (xe 7 chỗ)", 8_900_000, "Phim gốm cách nhiệt cho xe 7 chỗ, cản 99% tia UV."),
            new ProductSeed("film", "llumar", "Phim cách nhiệt Llumar IRX (xe 5 chỗ)", 9_900_000, "Công nghệ nano ceramic, cản nhiệt 60%, bảo hành 7 năm."),
            new ProductSeed("film", "llumar", "Phim cách nhiệt Llumar ATR (xe 5 chỗ)", 5_490_000, "Phim nhuộm màu kim loại, giá tốt, giảm chói hiệu quả."),
            new ProductSeed("film", "llumar", "Phim cách nhiệt Llumar IRX (xe 7 chỗ)", 11_490_000, "Nano ceramic cho xe 7 chỗ, không gây nhiễu GPS.")
    );

    // ------------------------------------------------------------------
    // Khách hàng (mật khẩu chung: DevDataSeeder.DEFAULT_PASSWORD)
    // ------------------------------------------------------------------

    static final List<CustomerSeed> CUSTOMERS = List.of(
            new CustomerSeed("nguyenvanan", "Nguyễn Văn An", "0901234567"),
            new CustomerSeed("tranthibich", "Trần Thị Bích", "0912345678"),
            new CustomerSeed("leminhkhoa", "Lê Minh Khoa", "0923456789"),
            new CustomerSeed("phamthuha", "Phạm Thu Hà", "0934567890"),
            new CustomerSeed("hoangquocbao", "Hoàng Quốc Bảo", "0945678901"),
            new CustomerSeed("vuthilan", "Vũ Thị Lan", "0956789012"),
            new CustomerSeed("dangtuananh", "Đặng Tuấn Anh", "0967890123"),
            new CustomerSeed("buithimai", "Bùi Thị Mai", "0978901234"),
            new CustomerSeed("dohuuthang", "Đỗ Hữu Thắng", "0989012345"),
            new CustomerSeed("ngothanhtam", "Ngô Thanh Tâm", "0390123456"),
            new CustomerSeed("lyhoanglong", "Lý Hoàng Long", "0381234567"),
            new CustomerSeed("trinhngoclinh", "Trịnh Ngọc Linh", "0372345678")
    );

    static final List<String> ADDRESSES = List.of(
            "12 Nguyễn Trãi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh",
            "245 Cách Mạng Tháng 8, Phường 4, Quận 3, TP. Hồ Chí Minh",
            "88 Phan Văn Trị, Phường 10, Quận Gò Vấp, TP. Hồ Chí Minh",
            "36 Lê Văn Việt, Phường Hiệp Phú, TP. Thủ Đức, TP. Hồ Chí Minh",
            "102 Trần Duy Hưng, Phường Trung Hòa, Quận Cầu Giấy, Hà Nội",
            "19 Láng Hạ, Phường Thành Công, Quận Ba Đình, Hà Nội",
            "57 Nguyễn Văn Linh, Phường Nam Dương, Quận Hải Châu, Đà Nẵng",
            "214 Lạch Tray, Phường Đằng Giang, Quận Ngô Quyền, Hải Phòng",
            "9 Trần Hưng Đạo, Phường Tân An, Quận Ninh Kiều, Cần Thơ",
            "71 Phạm Văn Thuận, Phường Tân Mai, TP. Biên Hòa, Đồng Nai"
    );

    static final List<String> ORDER_NOTES = List.of(
            "Giao giờ hành chính giúp mình",
            "Gọi trước khi giao 30 phút",
            "Cần xuất hóa đơn VAT",
            "Đặt lịch lắp đặt tại cửa hàng cuối tuần",
            "Giao buổi tối sau 18h"
    );

    // ------------------------------------------------------------------
    // Bài viết
    // ------------------------------------------------------------------

    static final List<String[]> POST_CATEGORIES = List.of(
            new String[]{"Kinh nghiệm lái xe", "Mẹo lái xe an toàn, tiết kiệm nhiên liệu và xử lý tình huống."},
            new String[]{"Đánh giá sản phẩm", "Review chi tiết camera hành trình, màn hình Android và phụ kiện."},
            new String[]{"Chăm sóc xe", "Hướng dẫn vệ sinh, bảo dưỡng để xe luôn như mới."},
            new String[]{"Tin khuyến mãi", "Chương trình ưu đãi và sự kiện của cửa hàng."}
    );

    static final List<PostSeed> POSTS = List.of(
            new PostSeed("Đánh giá sản phẩm", "So sánh 70mai A810 và Vietmap C61 Pro: Camera 4K nào đáng mua?",
                    "Hai mẫu camera hành trình 4K được quan tâm nhất hiện nay đối đầu trực tiếp.",
                    md("Camera hành trình 4K đang dần trở thành lựa chọn phổ biến.",
                            "Chất lượng hình ảnh", "Cả hai đều dùng cảm biến Sony STARVIS cho hình ảnh ban đêm rõ nét. **70mai A810** có HDR tốt hơn khi đi ngược sáng.",
                            "Tính năng cảnh báo", "**Vietmap C61 Pro** vượt trội với dữ liệu cảnh báo tốc độ, camera phạt nguội cập nhật cho đường Việt Nam.",
                            "Kết luận", "Nếu cần cảnh báo giao thông, chọn Vietmap. Nếu ưu tiên hình ảnh và giá tốt, chọn 70mai."), true),
            new PostSeed("Kinh nghiệm lái xe", "7 mẹo lái xe đường đèo an toàn cho người mới",
                    "Đường đèo dốc luôn là thử thách với tài xế mới, hãy nhớ kỹ những lưu ý sau.",
                    md("Lái xe đường đèo đòi hỏi sự tập trung và kỹ năng.",
                            "Sử dụng số thấp khi xuống dốc", "Dùng phanh động cơ thay vì rà phanh liên tục để tránh mất phanh do quá nhiệt.",
                            "Không vượt ở khúc cua", "Tầm nhìn hạn chế tại các khúc cua rất dễ gây tai nạn đối đầu.",
                            "Kiểm tra xe trước chuyến đi", "Kiểm tra áp suất lốp, má phanh, nước làm mát và đèn chiếu sáng."), true),
            new PostSeed("Chăm sóc xe", "Hướng dẫn rửa xe tại nhà đúng cách, không làm xước sơn",
                    "Rửa xe sai cách là nguyên nhân hàng đầu gây xước xoáy trên bề mặt sơn.",
                    md("Tự rửa xe tại nhà vừa tiết kiệm vừa thư giãn nếu làm đúng.",
                            "Chuẩn bị dụng cụ", "Hai xô nước, nước rửa xe chuyên dụng, găng tay microfiber và khăn lau thấm hút.",
                            "Quy trình hai xô", "Một xô chứa dung dịch, một xô nước sạch để giũ găng tay, tránh kéo cát bụi lên sơn.",
                            "Lau khô", "Dùng khăn microfiber lớn, thấm nhẹ thay vì chà mạnh."), true),
            new PostSeed("Đánh giá sản phẩm", "Có nên lắp màn hình Android cho xe đời cũ?",
                    "Màn hình Android giúp xe đời cũ có trải nghiệm như xe mới, nhưng cần lưu ý gì?",
                    md("Nhiều xe đời 2015-2019 vẫn dùng đầu CD hoặc màn hình zin đơn giản.",
                            "Lợi ích", "Dẫn đường, nghe nhạc trực tuyến, kết nối camera 360 và CarPlay.",
                            "Lưu ý khi chọn", "Chọn RAM tối thiểu 4GB, có mặt dưỡng đúng xe và chọn nơi lắp đặt uy tín để tránh lỗi điện."), true),
            new PostSeed("Chăm sóc xe", "Khi nào cần thay gạt mưa ô tô?",
                    "Gạt mưa hỏng ảnh hưởng trực tiếp đến tầm nhìn khi trời mưa.",
                    md("Gạt mưa là phụ tùng hao mòn thường bị bỏ quên.",
                            "Dấu hiệu cần thay", "Gạt để lại vệt nước, phát ra tiếng kêu, lưỡi cao su nứt hoặc cứng.",
                            "Chu kỳ thay", "Trung bình 6-12 tháng tùy điều kiện thời tiết và tần suất sử dụng."), true),
            new PostSeed("Kinh nghiệm lái xe", "Áp suất lốp bao nhiêu là chuẩn? Cách kiểm tra đơn giản",
                    "Lốp non hay căng quá đều gây hao xăng và mất an toàn.",
                    md("Áp suất lốp chuẩn được ghi ở khung cửa bên ghế lái.",
                            "Kiểm tra định kỳ", "Nên kiểm tra mỗi 2 tuần, khi lốp nguội.",
                            "Dùng cảm biến áp suất lốp", "Cảm biến TPMS giúp cảnh báo ngay khi lốp non hoặc thủng."), true),
            new PostSeed("Tin khuyến mãi", "Ưu đãi tháng 11: Giảm đến 20% camera hành trình",
                    "Chương trình khuyến mãi lớn nhất năm cho camera hành trình và màn hình Android.",
                    md("Từ ngày 01/11 đến 30/11, cửa hàng áp dụng ưu đãi đặc biệt.",
                            "Sản phẩm áp dụng", "Toàn bộ camera hành trình 70mai, Vietmap và màn hình Android Zestech.",
                            "Quà tặng kèm", "Tặng thẻ nhớ 64GB và miễn phí lắp đặt tại cửa hàng."), true),
            new PostSeed("Đánh giá sản phẩm", "Phim cách nhiệt 3M và Llumar: Loại nào tốt hơn?",
                    "So sánh hai thương hiệu phim cách nhiệt phổ biến nhất tại Việt Nam.",
                    md("Phim cách nhiệt giúp giảm nhiệt độ trong xe và bảo vệ nội thất.",
                            "Khả năng cách nhiệt", "3M Crystalline dẫn đầu về cản hồng ngoại, Llumar IRX có giá dễ tiếp cận hơn.",
                            "Bảo hành", "Cả hai đều có bảo hành điện tử chính hãng, kiểm tra kỹ tem trước khi dán."), true),
            new PostSeed("Chăm sóc xe", "Khử mùi nội thất ô tô hiệu quả với 5 cách đơn giản",
                    "Mùi hôi trong xe gây khó chịu, đặc biệt với xe thường xuyên đóng kín.",
                    md("Mùi trong xe thường đến từ thảm, điều hòa và đồ ăn.",
                            "Vệ sinh thảm và ghế", "Hút bụi và giặt thảm định kỳ hàng tháng.",
                            "Vệ sinh dàn lạnh", "Thay lọc gió điều hòa mỗi 10.000 km."), false),
            new PostSeed("Tin khuyến mãi", "Khai trương chi nhánh mới tại Thủ Đức",
                    "Chi nhánh mới với khu lắp đặt rộng rãi và nhiều ưu đãi khai trương.",
                    md("Chúng tôi chính thức khai trương chi nhánh thứ 3.",
                            "Ưu đãi khai trương", "Giảm 10% toàn bộ dịch vụ trong tuần đầu tiên."), false)
    );

    // ------------------------------------------------------------------
    // Dịch vụ
    // ------------------------------------------------------------------

    static final List<ServiceSeed> SERVICES = List.of(
            new ServiceSeed("Dán phim cách nhiệt",
                    "Dán phim cách nhiệt chính hãng 3M, Llumar với kỹ thuật viên nhiều năm kinh nghiệm.",
                    "Phim cách nhiệt giúp giảm nhiệt độ trong xe đến 60%, chống tia UV bảo vệ da và nội thất. Quy trình dán trong phòng kín, không bụi, đảm bảo phim không bọt khí.",
                    List.of("Phim chính hãng có tem bảo hành", "Thi công trong phòng sạch", "Bảo hành điện tử đến 10 năm", "Hoàn thành trong 3 giờ")),
            new ServiceSeed("Lắp đặt camera hành trình",
                    "Lắp camera hành trình đi dây âm trần gọn gàng, không ảnh hưởng hệ thống điện zin.",
                    "Kỹ thuật viên đi dây ẩn theo trần xe và cột A, đấu nguồn qua hộp cầu chì bằng chân cắm phụ, đảm bảo an toàn và thẩm mỹ.",
                    List.of("Đi dây âm hoàn toàn", "Không cắt nối dây zin", "Hướng dẫn sử dụng ứng dụng", "Bảo hành lắp đặt 12 tháng")),
            new ServiceSeed("Phủ ceramic bảo vệ sơn",
                    "Phủ ceramic 9H tạo lớp bảo vệ bền vững, hiệu ứng lá sen, xe luôn bóng đẹp.",
                    "Quy trình gồm rửa xe, tẩy bụi sắt, đánh bóng phục hồi sơn và phủ ceramic nhiều lớp. Lớp phủ giúp chống bám bẩn, chống tia UV và hạn chế xước nhẹ.",
                    List.of("Độ cứng 9H", "Hiệu ứng lá sen", "Độ bền 2-3 năm", "Bảo dưỡng định kỳ miễn phí")),
            new ServiceSeed("Lắp đặt màn hình Android",
                    "Lắp màn hình Android kèm mặt dưỡng đúng xe, giữ nguyên chức năng vô lăng.",
                    "Sử dụng giắc zin theo xe, giữ nguyên điều khiển vô lăng, camera lùi và cảm biến. Cài đặt sẵn bản đồ, ứng dụng giải trí theo yêu cầu.",
                    List.of("Giắc cắm zin theo xe", "Giữ điều khiển vô lăng", "Cài đặt ứng dụng miễn phí", "Bảo hành 18 tháng")),
            new ServiceSeed("Vệ sinh nội thất chuyên sâu",
                    "Làm sạch toàn bộ nội thất, khử mùi, diệt khuẩn bằng hơi nước nóng.",
                    "Tháo ghế khi cần thiết, giặt thảm trần, vệ sinh ghế da và dưỡng da, xông khử mùi ozone. Phù hợp xe lâu ngày chưa vệ sinh hoặc xe mới mua lại.",
                    List.of("Hơi nước nóng diệt khuẩn", "Dưỡng da ghế", "Khử mùi bằng ozone", "Hoàn thành trong ngày"))
    );

    // ------------------------------------------------------------------
    // Banner
    // ------------------------------------------------------------------

    static final List<BannerSeed> BANNERS = List.of(
            new BannerSeed("Camera hành trình 4K", "Ghi lại mọi khoảnh khắc trên đường — giảm đến 20% trong tháng này", "product:Camera hành trình 70mai A810 4K", "Mua ngay", "0f172a"),
            new BannerSeed("Màn hình Android thông minh", "Nâng cấp xe cũ thành xe mới với CarPlay và camera 360", "product:Màn hình Android Zestech Z800 Pro+", "Khám phá", "1e3a8a"),
            new BannerSeed("Dán phim cách nhiệt chính hãng", "3M, Llumar — bảo hành điện tử đến 10 năm", "/services/dan-phim-cach-nhiet", "Đặt lịch", "164e63"),
            new BannerSeed("Cẩm nang chăm sóc xe", "Mẹo hay giúp xe luôn bền đẹp như mới", "/blog", "Đọc ngay", "065f46")
    );

    // ------------------------------------------------------------------
    // Live chat (mỗi tin nhắn: {senderType, content})
    // ------------------------------------------------------------------

    static final List<ChatSeed> CHATS = List.of(
            new ChatSeed("Anh Tuấn", "OPEN", List.of(
                    new String[]{"CUSTOMER", "Shop ơi, camera 70mai A810 có lắp được cho Mazda 3 không?"},
                    new String[]{"ADMIN", "Dạ được anh nhé, bên em có lắp đặt đi dây âm miễn phí ạ."},
                    new String[]{"CUSTOMER", "Lắp mất bao lâu vậy shop?"})),
            new ChatSeed("Chị Hương", "OPEN", List.of(
                    new String[]{"CUSTOMER", "Cho mình hỏi phim cách nhiệt 3M Crystalline xe 7 chỗ giá bao nhiêu?"},
                    new String[]{"CUSTOMER", "Có bảo hành bao lâu ạ?"})),
            new ChatSeed("Khách vãng lai", "OPEN", List.<String[]>of(
                    new String[]{"CUSTOMER", "Shop có bán thảm lót sàn cho Toyota Cross không?"})),
            new ChatSeed("Anh Minh", "CLOSED", List.of(
                    new String[]{"CUSTOMER", "Đơn hàng của mình bao giờ giao vậy shop?"},
                    new String[]{"ADMIN", "Dạ đơn của anh đã giao cho đơn vị vận chuyển, dự kiến nhận ngày mai ạ."},
                    new String[]{"CUSTOMER", "Ok cảm ơn shop."},
                    new String[]{"ADMIN", "Dạ cảm ơn anh đã ủng hộ cửa hàng!"})),
            new ChatSeed("Chị Lan", "CLOSED", List.of(
                    new String[]{"CUSTOMER", "Màn hình Zestech Z800 Pro+ có CarPlay không dây không?"},
                    new String[]{"ADMIN", "Dạ có chị, hỗ trợ cả CarPlay và Android Auto không dây ạ."}))
    );

    /** Ghép bài viết Markdown đơn giản: đoạn mở đầu + các cặp (tiêu đề, nội dung). */
    private static String md(String intro, String... sections) {
        StringBuilder sb = new StringBuilder(intro).append("\n\n");
        for (int i = 0; i + 1 < sections.length; i += 2) {
            sb.append("## ").append(sections[i]).append("\n\n").append(sections[i + 1]).append("\n\n");
        }
        return sb.toString().trim();
    }
}

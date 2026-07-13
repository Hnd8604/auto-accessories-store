import { useEffect, useState, useRef, memo, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import hero1 from "@/assets/hero-interior.jpg";
import hero2 from "@/assets/seats.jpg";
import hero3 from "@/assets/steering-wheel.jpg";
import hero4 from "@/assets/dashboard.jpg";
import { BannersApi } from "@/features/banners/api";
import { ChevronLeft, ChevronRight, ArrowRight, Phone } from "lucide-react";

interface Slide {
  image: string;
  title: string;
  subtitle: string;
  desc: string;
  cta: string;
  ctaSecondary?: string;
  redirectUrl?: string;
}

const defaultSlides: Slide[] = [
  {
    image: hero1,
    title: "Nâng Tầm Đẳng Cấp",
    subtitle: "NỘI THẤT XE HƠI",
    desc: "Thiết kế – độ nội thất cao cấp với da thật, carbon fiber và công nghệ hiện đại.",
    cta: "Khám Phá Dịch Vụ",
    ctaSecondary: "Tư Vấn Miễn Phí",
  },
  {
    image: hero2,
    title: "Bọc Ghế Da",
    subtitle: "CAO CẤP",
    desc: "Da Nappa, Alcantara – chuẩn sang trọng cho khoang lái của bạn.",
    cta: "Xem Bảng Giá",
    ctaSecondary: "Liên Hệ Ngay",
  },
  {
    image: hero3,
    title: "Độ Vô Lăng",
    subtitle: "CẦM NẮM HOÀN HẢO",
    desc: "Cá nhân hoá chất liệu và đường chỉ theo phong cách của bạn.",
    cta: "Tư Vấn Ngay",
    ctaSecondary: "Xem Mẫu",
  },
  {
    image: hero4,
    title: "Carbon Fiber",
    subtitle: "& LED AMBIENT",
    desc: "Nâng cấp taplo, ốp nội thất và hệ thống đèn nội thất ấn tượng.",
    cta: "Xem Thư Viện Ảnh",
    ctaSecondary: "Đặt Lịch Hẹn",
  },
];

const AUTOPLAY_INTERVAL = 6000;

export const HeroSlider = memo(() => {
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isHovered, setIsHovered] = useState(false);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [progress, setProgress] = useState(0);
  const progressRef = useRef<number>(0);
  const animFrameRef = useRef<number>(0);
  const lastTimeRef = useRef<number>(0);
  const navigate = useNavigate();

  // Fetch active banners from API
  const { data: bannersData } = useQuery({
    queryKey: ["active-banners"],
    queryFn: BannersApi.getActive,
    staleTime: 5 * 60 * 1000,
  });

  // Convert banners to slides format
  const apiSlides: Slide[] = (bannersData?.result || []).map((banner) => ({
    image: banner.imageUrl,
    title: banner.title || "AutoLux Interior",
    subtitle: banner.subtitle || "",
    desc: banner.altText || "",
    cta: banner.buttonText || "Tìm Hiểu Thêm",
    redirectUrl: banner.redirectUrl,
  }));

  const slides = apiSlides.length > 0 ? apiSlides : defaultSlides;

  const goToSlide = useCallback(
    (index: number) => {
      if (isTransitioning) return;
      setIsTransitioning(true);
      setCurrentSlide(index);
      progressRef.current = 0;
      setProgress(0);
      lastTimeRef.current = 0;
      setTimeout(() => setIsTransitioning(false), 800);
    },
    [isTransitioning]
  );

  const goNext = useCallback(() => {
    goToSlide((currentSlide + 1) % slides.length);
  }, [currentSlide, slides.length, goToSlide]);

  const goPrev = useCallback(() => {
    goToSlide((currentSlide - 1 + slides.length) % slides.length);
  }, [currentSlide, slides.length, goToSlide]);

  // Autoplay with smooth progress
  useEffect(() => {
    if (isHovered) return;

    const animate = (time: number) => {
      if (!lastTimeRef.current) lastTimeRef.current = time;
      const delta = time - lastTimeRef.current;
      lastTimeRef.current = time;

      progressRef.current += delta;
      const pct = Math.min((progressRef.current / AUTOPLAY_INTERVAL) * 100, 100);
      setProgress(pct);

      if (progressRef.current >= AUTOPLAY_INTERVAL) {
        progressRef.current = 0;
        lastTimeRef.current = 0;
        setProgress(0);
        setCurrentSlide((prev) => (prev + 1) % slides.length);
      }

      animFrameRef.current = requestAnimationFrame(animate);
    };

    animFrameRef.current = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animFrameRef.current);
  }, [isHovered, slides.length, currentSlide]);

  // Reset progress on hover
  useEffect(() => {
    if (isHovered) {
      lastTimeRef.current = 0;
    }
  }, [isHovered]);

  const s = slides[currentSlide];

  return (
    <section
      id="hero-slider"
      className="hero-slider"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Background Images with Ken Burns */}
      {slides.map((slide, idx) => (
        <div
          key={idx}
          className={`hero-slider__bg ${currentSlide === idx ? "hero-slider__bg--active" : ""}`}
        >
          <img
            src={slide.image}
            alt={slide.title}
            className="hero-slider__bg-img"
            loading={idx === 0 ? "eager" : "lazy"}
          />
        </div>
      ))}

      {/* Overlay Gradients */}
      <div className="hero-slider__overlay" />
      <div className="hero-slider__overlay-bottom" />
      <div className="hero-slider__vignette" />

      {/* Decorative Elements */}
      <div className="hero-slider__deco-line hero-slider__deco-line--left" />
      <div className="hero-slider__deco-corner hero-slider__deco-corner--tl" />
      <div className="hero-slider__deco-corner hero-slider__deco-corner--br" />

      {/* Floating Particles */}
      <div className="hero-slider__particles">
        <span className="hero-slider__particle" style={{ left: '10%', animationDelay: '0s' }} />
        <span className="hero-slider__particle" style={{ left: '25%', animationDelay: '2s' }} />
        <span className="hero-slider__particle" style={{ left: '60%', animationDelay: '4s' }} />
        <span className="hero-slider__particle" style={{ left: '80%', animationDelay: '1s' }} />
        <span className="hero-slider__particle" style={{ left: '45%', animationDelay: '3s' }} />
      </div>

      {/* Content */}
      <div className="hero-slider__content">
        <div className="hero-slider__text-container">
          {/* Badge/Tag */}
          <div
            className={`hero-slider__badge ${currentSlide === slides.indexOf(s) ? "hero-slider__animate" : ""}`}
          >
            <span className="hero-slider__badge-dot" />
            PREMIUM QUALITY
          </div>

          {/* Title */}
          <h1
            className={`hero-slider__title ${currentSlide === slides.indexOf(s) ? "hero-slider__animate hero-slider__animate--delay-1" : ""}`}
          >
            <span className="hero-slider__title-main">{s.title}</span>
            <span className="hero-slider__title-accent">{s.subtitle}</span>
          </h1>

          {/* Description */}
          {s.desc && (
            <p
              className={`hero-slider__desc ${currentSlide === slides.indexOf(s) ? "hero-slider__animate hero-slider__animate--delay-2" : ""}`}
            >
              {s.desc}
            </p>
          )}

          {/* CTA Buttons */}
          <div
            className={`hero-slider__cta-group ${currentSlide === slides.indexOf(s) ? "hero-slider__animate hero-slider__animate--delay-3" : ""}`}
          >
            <button
              className="hero-slider__cta-primary"
              onClick={() => {
                if (s.redirectUrl) navigate(s.redirectUrl);
              }}
            >
              <span>{s.cta}</span>
              <ArrowRight className="hero-slider__cta-icon" />
            </button>
            {s.ctaSecondary && (
              <button className="hero-slider__cta-secondary">
                <Phone className="hero-slider__cta-icon-phone" />
                <span>{s.ctaSecondary}</span>
              </button>
            )}
          </div>
        </div>

        {/* Right Side - Slide Counter */}
        <div className="hero-slider__counter">
          <span className="hero-slider__counter-current">
            {String(currentSlide + 1).padStart(2, "0")}
          </span>
          <span className="hero-slider__counter-separator" />
          <span className="hero-slider__counter-total">
            {String(slides.length).padStart(2, "0")}
          </span>
        </div>
      </div>

      {/* Navigation Arrows */}
      {slides.length > 1 && (
        <>
          <button
            className="hero-slider__nav hero-slider__nav--prev"
            onClick={goPrev}
            aria-label="Slide trước"
          >
            <ChevronLeft className="hero-slider__nav-icon" />
          </button>
          <button
            className="hero-slider__nav hero-slider__nav--next"
            onClick={goNext}
            aria-label="Slide tiếp"
          >
            <ChevronRight className="hero-slider__nav-icon" />
          </button>
        </>
      )}

      {/* Bottom Bar: Dots with Progress */}
      {slides.length > 1 && (
        <div className="hero-slider__bottom-bar">
          <div className="hero-slider__dots">
            {slides.map((_, idx) => (
              <button
                key={idx}
                onClick={() => goToSlide(idx)}
                className={`hero-slider__dot ${currentSlide === idx ? "hero-slider__dot--active" : ""}`}
                aria-label={`Đến slide ${idx + 1}`}
              >
                {currentSlide === idx && (
                  <span
                    className="hero-slider__dot-progress"
                    style={{ width: `${progress}%` }}
                  />
                )}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Scroll Indicator */}
      <div className="hero-slider__scroll-hint">
        <div className="hero-slider__scroll-mouse">
          <div className="hero-slider__scroll-wheel" />
        </div>
        <span className="hero-slider__scroll-text">Cuộn xuống</span>
      </div>

      <style>{`
        .hero-slider {
          position: relative;
          width: 100%;
          height: 100vh;
          min-height: 600px;
          overflow: hidden;
          background: #0a0a0a;
        }

        /* ===== Background Images ===== */
        .hero-slider__bg {
          position: absolute;
          inset: 0;
          opacity: 0;
          transition: opacity 1s cubic-bezier(0.4, 0, 0.2, 1);
          z-index: 1;
        }
        .hero-slider__bg--active {
          opacity: 1;
        }
        .hero-slider__bg-img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          animation: kenBurns 20s ease-in-out infinite alternate;
          will-change: transform;
        }
        .hero-slider__bg--active .hero-slider__bg-img {
          animation: kenBurns 20s ease-in-out infinite alternate;
        }

        @keyframes kenBurns {
          0% { transform: scale(1) translate(0, 0); }
          100% { transform: scale(1.08) translate(-1%, -1%); }
        }

        /* ===== Overlays ===== */
        .hero-slider__overlay {
          position: absolute;
          inset: 0;
          z-index: 2;
          background: linear-gradient(
            135deg,
            rgba(0, 0, 0, 0.85) 0%,
            rgba(0, 0, 0, 0.55) 40%,
            rgba(0, 0, 0, 0.25) 70%,
            rgba(0, 0, 0, 0.15) 100%
          );
        }
        .hero-slider__overlay-bottom {
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 200px;
          z-index: 2;
          background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, transparent 100%);
        }
        .hero-slider__vignette {
          position: absolute;
          inset: 0;
          z-index: 2;
          box-shadow: inset 0 0 150px rgba(0, 0, 0, 0.4);
          pointer-events: none;
        }

        /* ===== Decorative Elements ===== */
        .hero-slider__deco-line--left {
          position: absolute;
          left: 40px;
          top: 180px;
          bottom: 120px;
          width: 2px;
          z-index: 5;
          background: linear-gradient(
            to bottom,
            transparent,
            hsl(var(--primary) / 0.6) 30%,
            hsl(var(--primary) / 0.6) 70%,
            transparent
          );
          opacity: 0.5;
        }

        .hero-slider__deco-corner {
          position: absolute;
          width: 40px;
          height: 40px;
          z-index: 5;
          opacity: 0.3;
        }
        .hero-slider__deco-corner--tl {
          top: 180px;
          left: 30px;
          border-top: 2px solid hsl(var(--primary) / 0.8);
          border-left: 2px solid hsl(var(--primary) / 0.8);
        }
        .hero-slider__deco-corner--br {
          bottom: 120px;
          right: 40px;
          border-bottom: 2px solid hsl(var(--primary) / 0.4);
          border-right: 2px solid hsl(var(--primary) / 0.4);
        }

        /* ===== Particles ===== */
        .hero-slider__particles {
          position: absolute;
          inset: 0;
          z-index: 3;
          pointer-events: none;
          overflow: hidden;
        }
        .hero-slider__particle {
          position: absolute;
          bottom: -10px;
          width: 2px;
          height: 2px;
          background: hsl(var(--primary) / 0.4);
          border-radius: 50%;
          animation: floatUp 8s linear infinite;
        }
        @keyframes floatUp {
          0% {
            transform: translateY(0) scale(1);
            opacity: 0;
          }
          10% { opacity: 0.6; }
          90% { opacity: 0.2; }
          100% {
            transform: translateY(-100vh) scale(0.5);
            opacity: 0;
          }
        }

        /* ===== Content ===== */
        .hero-slider__content {
          position: absolute;
          inset: 0;
          z-index: 10;
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 0 80px;
          padding-top: 80px;
        }

        .hero-slider__text-container {
          max-width: 640px;
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        /* Badge */
        .hero-slider__badge {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          font-size: 11px;
          font-weight: 600;
          letter-spacing: 3px;
          color: hsl(var(--primary));
          text-transform: uppercase;
          padding: 8px 16px;
          background: hsl(var(--primary) / 0.08);
          border: 1px solid hsl(var(--primary) / 0.2);
          border-radius: 4px;
          width: fit-content;
          backdrop-filter: blur(8px);
        }
        .hero-slider__badge-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: hsl(var(--primary));
          animation: pulseDot 2s ease-in-out infinite;
        }
        @keyframes pulseDot {
          0%, 100% { opacity: 1; transform: scale(1); }
          50% { opacity: 0.5; transform: scale(0.8); }
        }

        /* Title */
        .hero-slider__title {
          display: flex;
          flex-direction: column;
          gap: 4px;
          line-height: 1.1;
        }
        .hero-slider__title-main {
          font-size: clamp(2.5rem, 5vw, 4rem);
          font-weight: 800;
          color: white;
          text-shadow: 0 2px 20px rgba(0, 0, 0, 0.5);
        }
        .hero-slider__title-accent {
          font-size: clamp(2.5rem, 5vw, 4rem);
          font-weight: 800;
          background: linear-gradient(135deg, hsl(var(--primary)) 0%, hsl(var(--accent)) 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          text-shadow: none;
        }

        /* Description */
        .hero-slider__desc {
          font-size: clamp(1rem, 1.5vw, 1.2rem);
          color: rgba(255, 255, 255, 0.75);
          line-height: 1.7;
          max-width: 520px;
        }

        /* CTA Buttons */
        .hero-slider__cta-group {
          display: flex;
          gap: 16px;
          flex-wrap: wrap;
          padding-top: 8px;
        }

        .hero-slider__cta-primary {
          display: inline-flex;
          align-items: center;
          gap: 10px;
          padding: 14px 32px;
          font-size: 14px;
          font-weight: 600;
          color: white;
          background: linear-gradient(135deg, hsl(var(--primary)) 0%, hsl(var(--primary) / 0.8) 100%);
          border: none;
          border-radius: 6px;
          cursor: pointer;
          transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
          position: relative;
          overflow: hidden;
          letter-spacing: 0.5px;
        }
        .hero-slider__cta-primary::before {
          content: '';
          position: absolute;
          inset: 0;
          background: linear-gradient(135deg, hsl(var(--accent)) 0%, hsl(var(--primary)) 100%);
          opacity: 0;
          transition: opacity 0.4s ease;
        }
        .hero-slider__cta-primary:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 30px hsl(var(--primary) / 0.4);
        }
        .hero-slider__cta-primary:hover::before {
          opacity: 1;
        }
        .hero-slider__cta-primary span,
        .hero-slider__cta-primary .hero-slider__cta-icon {
          position: relative;
          z-index: 1;
        }
        .hero-slider__cta-icon {
          width: 16px;
          height: 16px;
          transition: transform 0.3s ease;
        }
        .hero-slider__cta-primary:hover .hero-slider__cta-icon {
          transform: translateX(4px);
        }

        .hero-slider__cta-secondary {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          padding: 14px 28px;
          font-size: 14px;
          font-weight: 500;
          color: rgba(255, 255, 255, 0.9);
          background: rgba(255, 255, 255, 0.06);
          border: 1px solid rgba(255, 255, 255, 0.15);
          border-radius: 6px;
          cursor: pointer;
          transition: all 0.3s ease;
          backdrop-filter: blur(10px);
          letter-spacing: 0.3px;
        }
        .hero-slider__cta-secondary:hover {
          background: rgba(255, 255, 255, 0.12);
          border-color: rgba(255, 255, 255, 0.3);
          transform: translateY(-2px);
        }
        .hero-slider__cta-icon-phone {
          width: 14px;
          height: 14px;
        }

        /* ===== Slide Counter ===== */
        .hero-slider__counter {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
          user-select: none;
        }
        .hero-slider__counter-current {
          font-size: 4rem;
          font-weight: 800;
          color: white;
          line-height: 1;
          text-shadow: 0 2px 20px rgba(0,0,0,0.3);
        }
        .hero-slider__counter-separator {
          width: 2px;
          height: 40px;
          background: linear-gradient(to bottom, hsl(var(--primary)), transparent);
        }
        .hero-slider__counter-total {
          font-size: 1.2rem;
          font-weight: 400;
          color: rgba(255, 255, 255, 0.4);
          letter-spacing: 2px;
        }

        /* ===== Navigation Arrows ===== */
        .hero-slider__nav {
          position: absolute;
          top: 50%;
          transform: translateY(-50%);
          z-index: 15;
          width: 52px;
          height: 52px;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 1px solid rgba(255, 255, 255, 0.15);
          border-radius: 50%;
          background: rgba(0, 0, 0, 0.2);
          backdrop-filter: blur(12px);
          cursor: pointer;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          color: rgba(255, 255, 255, 0.7);
          opacity: 0;
        }
        .hero-slider:hover .hero-slider__nav {
          opacity: 1;
        }
        .hero-slider__nav:hover {
          background: hsl(var(--primary) / 0.3);
          border-color: hsl(var(--primary) / 0.5);
          color: white;
          transform: translateY(-50%) scale(1.1);
        }
        .hero-slider__nav--prev { left: 24px; }
        .hero-slider__nav--next { right: 24px; }
        .hero-slider__nav-icon {
          width: 22px;
          height: 22px;
        }

        /* ===== Bottom Bar / Dots ===== */
        .hero-slider__bottom-bar {
          position: absolute;
          bottom: 40px;
          left: 50%;
          transform: translateX(-50%);
          z-index: 15;
          display: flex;
          align-items: center;
          gap: 16px;
        }
        .hero-slider__dots {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .hero-slider__dot {
          position: relative;
          width: 40px;
          height: 3px;
          border-radius: 3px;
          background: rgba(255, 255, 255, 0.2);
          border: none;
          cursor: pointer;
          overflow: hidden;
          transition: all 0.4s ease;
          padding: 0;
        }
        .hero-slider__dot:hover {
          background: rgba(255, 255, 255, 0.35);
        }
        .hero-slider__dot--active {
          width: 64px;
          background: rgba(255, 255, 255, 0.15);
        }
        .hero-slider__dot-progress {
          position: absolute;
          left: 0;
          top: 0;
          height: 100%;
          background: linear-gradient(90deg, hsl(var(--primary)), hsl(var(--accent)));
          border-radius: 3px;
          transition: width 0.05s linear;
        }

        /* ===== Scroll Indicator ===== */
        .hero-slider__scroll-hint {
          position: absolute;
          bottom: 40px;
          right: 80px;
          z-index: 15;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
          opacity: 0.5;
          animation: fadeInUp 1s ease 2s both;
        }
        .hero-slider__scroll-mouse {
          width: 20px;
          height: 32px;
          border: 2px solid rgba(255, 255, 255, 0.4);
          border-radius: 12px;
          position: relative;
        }
        .hero-slider__scroll-wheel {
          position: absolute;
          top: 6px;
          left: 50%;
          transform: translateX(-50%);
          width: 3px;
          height: 6px;
          border-radius: 3px;
          background: rgba(255, 255, 255, 0.6);
          animation: scrollWheel 2s ease-in-out infinite;
        }
        .hero-slider__scroll-text {
          font-size: 10px;
          letter-spacing: 2px;
          text-transform: uppercase;
          color: rgba(255, 255, 255, 0.4);
        }

        @keyframes scrollWheel {
          0% { transform: translateX(-50%) translateY(0); opacity: 1; }
          100% { transform: translateX(-50%) translateY(10px); opacity: 0; }
        }

        /* ===== Animations ===== */
        .hero-slider__animate {
          animation: slideUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
        }
        .hero-slider__animate--delay-1 {
          animation-delay: 0.15s;
        }
        .hero-slider__animate--delay-2 {
          animation-delay: 0.3s;
        }
        .hero-slider__animate--delay-3 {
          animation-delay: 0.45s;
        }

        @keyframes slideUp {
          0% {
            opacity: 0;
            transform: translateY(40px);
          }
          100% {
            opacity: 1;
            transform: translateY(0);
          }
        }
        @keyframes fadeInUp {
          0% {
            opacity: 0;
            transform: translateY(10px);
          }
          100% {
            opacity: 0.5;
            transform: translateY(0);
          }
        }

        /* ===== Responsive ===== */
        @media (max-width: 1024px) {
          .hero-slider__content {
            padding: 0 40px;
            padding-top: 100px;
          }
          .hero-slider__counter {
            display: none;
          }
          .hero-slider__deco-line--left {
            left: 20px;
          }
          .hero-slider__deco-corner--tl {
            left: 12px;
          }
          .hero-slider__scroll-hint {
            right: 40px;
          }
        }

        @media (max-width: 768px) {
          .hero-slider {
            height: 100svh;
            min-height: 500px;
          }
          .hero-slider__content {
            padding: 0 24px;
            padding-top: 120px;
            align-items: flex-end;
            padding-bottom: 100px;
          }
          .hero-slider__text-container {
            gap: 14px;
          }
          .hero-slider__title-main,
          .hero-slider__title-accent {
            font-size: clamp(1.8rem, 7vw, 2.5rem);
          }
          .hero-slider__desc {
            font-size: 0.95rem;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }
          .hero-slider__cta-group {
            flex-direction: column;
            gap: 10px;
          }
          .hero-slider__cta-primary,
          .hero-slider__cta-secondary {
            width: 100%;
            justify-content: center;
          }
          .hero-slider__nav {
            width: 40px;
            height: 40px;
          }
          .hero-slider__nav--prev { left: 12px; }
          .hero-slider__nav--next { right: 12px; }
          .hero-slider__nav-icon {
            width: 18px;
            height: 18px;
          }
          .hero-slider__deco-line--left,
          .hero-slider__deco-corner,
          .hero-slider__scroll-hint {
            display: none;
          }
          .hero-slider__bottom-bar {
            bottom: 24px;
          }
          .hero-slider__dot {
            width: 28px;
          }
          .hero-slider__dot--active {
            width: 48px;
          }
          .hero-slider__badge {
            font-size: 10px;
            letter-spacing: 2px;
            padding: 6px 12px;
          }
        }

        @media (max-width: 480px) {
          .hero-slider__title-main,
          .hero-slider__title-accent {
            font-size: clamp(1.5rem, 6vw, 2rem);
          }
          .hero-slider__overlay {
            background: linear-gradient(
              180deg,
              rgba(0, 0, 0, 0.7) 0%,
              rgba(0, 0, 0, 0.4) 40%,
              rgba(0, 0, 0, 0.5) 70%,
              rgba(0, 0, 0, 0.8) 100%
            );
          }
        }
      `}</style>
    </section>
  );
});

HeroSlider.displayName = "HeroSlider";

export default HeroSlider;

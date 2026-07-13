import DOMPurify from 'dompurify';
import { marked } from 'marked';

export const renderContent = (content: string): string => {
  if (!content || content.trim() === '') {
    return '';
  }

  // Configure marked options
  marked.setOptions({
    breaks: true,
    gfm: true,
    mangle: false,
    headerIds: true,
    pedantic: false,
  });

  // Convert Markdown to HTML - marked.parse is synchronous by default
  let htmlContent: string;
  try {
    htmlContent = marked.parse(content) as string;
  } catch (error) {
    console.error('Error parsing markdown:', error);
    return `<p>${content}</p>`;
  }
  
  // Post-process: Find images followed by emphasized text (caption)
  // Pattern: <p><img ... />(<br>)?<em>caption text</em></p>
  // The <br> appears because of breaks: true option
  // Replace with: <p class="image-wrapper"><img ... /></p><p class="image-caption">caption text</p>
  htmlContent = htmlContent.replace(
    /<p>(<img[^>]*>)(?:<br>)?\s*<em>([^<]*)<\/em><\/p>/gi,
    '<p class="image-wrapper">$1</p><p class="image-caption">$2</p>'
  );
  
  // Sanitize HTML to prevent XSS attacks
  const cleanHtml = DOMPurify.sanitize(htmlContent, {
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'u', 's', 'b', 'i',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li',
      'blockquote', 'code', 'pre',
      'a', 'img', 'iframe', 'video', 'source',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'div', 'span', 'hr'
    ],
    ALLOWED_ATTR: [
      'href', 'src', 'alt', 'title', 'class', 'style',
      'width', 'height', 'controls', 'autoplay', 'loop',
      'allow', 'allowfullscreen', 'frameborder', 'scrolling',
      'type', 'target', 'rel', 'id'
    ],
    KEEP_CONTENT: true,
    RETURN_DOM: false,
    RETURN_DOM_FRAGMENT: false,
  });
  
  return cleanHtml;
};

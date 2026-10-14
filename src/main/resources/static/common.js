/* Shared by every page: CSRF header on writes, safe HTML escaping, POST logout. */
(function () {
  const nativeFetch = window.fetch.bind(window);

  function csrfToken() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : '';
  }

  window.fetch = function (url, opts) {
    opts = opts || {};
    const method = (opts.method || 'GET').toUpperCase();
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
      opts.headers = Object.assign({}, opts.headers || {}, { 'X-XSRF-TOKEN': csrfToken() });
    }
    return nativeFetch(url, opts);
  };

  /** Escape text before putting it in an innerHTML template. */
  window.esc = function (value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  };

  // Spring Security requires POST for logout when CSRF protection is on.
  document.addEventListener('click', function (e) {
    const link = e.target.closest && e.target.closest('a[href="/logout"]');
    if (!link) return;
    e.preventDefault();
    nativeFetch('/logout', { method: 'POST', headers: { 'X-XSRF-TOKEN': csrfToken() } })
      .finally(function () { window.location.href = '/login.html'; });
  });
})();

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
// Render a styled visual badge based on the user's role
window.renderRoleBadge = function(role) {
    switch(role) {
        case 'ROLE_MANAGER':
            return `<span style="display:inline-flex; align-items:center; gap:6px; padding:3px 10px; font-size:11px; font-weight:700; background:rgba(147, 51, 234, 0.15); color:#c084fc; border:1px solid rgba(147, 51, 234, 0.35); border-radius:9999px;">
                        🛡️ Manager / Supervisor
                    </span>`;
        case 'ROLE_ANALYST':
            return `<span style="display:inline-flex; align-items:center; gap:6px; padding:3px 10px; font-size:11px; font-weight:700; background:rgba(59, 130, 246, 0.15); color:#60a5fa; border:1px solid rgba(59, 130, 246, 0.35); border-radius:9999px;">
                        🔍 SOC Analyst
                    </span>`;
        case 'ROLE_EMPLOYEE':
        default:
            return `<span style="display:inline-flex; align-items:center; gap:6px; padding:3px 10px; font-size:11px; font-weight:700; background:rgba(16, 185, 129, 0.15); color:#34d399; border:1px solid rgba(16, 185, 129, 0.35); border-radius:9999px;">
                        👤 Operative Trainee
                    </span>`;
    }
};

// Automatically populate user info and role badge if header elements exist
document.addEventListener('DOMContentLoaded', async () => {
    try {
        const res = await fetch('/api/quest/my-stats');
        if (!res.ok) return;
        const data = await res.json();
        
        const roleContainer = document.getElementById('userRoleBadge');
        if (roleContainer && data.role) {
            roleContainer.innerHTML = window.renderRoleBadge(data.role);
        }
    } catch (e) {
        // Fallback silently if stats endpoint isn't present on current page
    }
});
async function applyRoleUIRestrictions() {
    try {
        const res = await fetch('/api/quest/my-stats');
        if (!res.ok) return;
        const data = await res.json();
        
        // Hide manager-only elements for standard employees
        if (data.role === 'ROLE_EMPLOYEE') {
            document.querySelectorAll('.manager-only').forEach(el => el.style.display = 'none');
        }
    } catch (e) {}
}
document.addEventListener('DOMContentLoaded', applyRoleUIRestrictions);
})();

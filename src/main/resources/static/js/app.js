/* =====================================================
   app.js — JavaScript thuần cho hệ thống quản lý đề tài
   Tính năng:
     - Mở/đóng sidebar trên màn hình nhỏ
     - Tự đóng flash sau 5 giây
     - Modal xác nhận (data-confirm trên form)
     - Toggle trường theo periodType
     - Ẩn/hiện mật khẩu
     - Chống bấm đúp nút submit
     - Tìm nhanh SV trong danh sách chọn
     - Tìm kiếm/lọc/phân trang phía trình duyệt cho bảng
   Không phụ thuộc thư viện ngoài.
===================================================== */
(function () {
    'use strict';

    // ========== SIDEBAR TOGGLE ==========
    function initSidebar() {
        var toggle = document.querySelector('.topbar-toggle');
        var sidebar = document.querySelector('.sidebar');
        if (!toggle || !sidebar) return;

        toggle.addEventListener('click', function () {
            sidebar.classList.toggle('open');
            document.body.classList.toggle('sidebar-open', sidebar.classList.contains('open'));
        });

        // Đóng khi bấm link trên mobile
        sidebar.addEventListener('click', function (e) {
            var link = e.target.closest('.nav-link');
            if (link && window.innerWidth <= 768) {
                sidebar.classList.remove('open');
                document.body.classList.remove('sidebar-open');
            }
        });

        // Đóng khi bấm ngoài
        document.addEventListener('click', function (e) {
            if (window.innerWidth > 768) return;
            if (!sidebar.contains(e.target) && !toggle.contains(e.target)) {
                sidebar.classList.remove('open');
                document.body.classList.remove('sidebar-open');
            }
        });

        // Đóng khi nhấn Escape
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && sidebar.classList.contains('open') && window.innerWidth <= 768) {
                sidebar.classList.remove('open');
                document.body.classList.remove('sidebar-open');
                toggle.focus();
            }
        });
    }

    // ========== FLASH AUTO-CLOSE ==========
    function initFlash() {
        var alerts = document.querySelectorAll('.alert[data-auto-close]');
        alerts.forEach(function (alert) {
            var delay = parseInt(alert.dataset.autoClose || '5000', 10);
            setTimeout(function () {
                closeAlert(alert);
            }, delay);
        });

        document.querySelectorAll('.alert .alert-close').forEach(function (btn) {
            btn.addEventListener('click', function () {
                closeAlert(btn.closest('.alert'));
            });
        });
    }

    function closeAlert(alert) {
        if (!alert || alert.style.display === 'none') return;
        alert.style.transition = 'opacity .3s ease, transform .3s ease';
        alert.style.opacity = '0';
        alert.style.transform = 'translateY(-4px)';
        setTimeout(function () {
            if (alert.parentNode) {
                alert.style.display = 'none';
            }
        }, 320);
    }

    // ========== MODAL XÁC NHẬN ==========
    function initConfirmModal() {
        document.addEventListener('submit', function (e) {
            var form = e.target;
            if (!(form instanceof HTMLFormElement)) return;
            var msg = form.dataset.confirm;
            if (!msg) return;

            e.preventDefault();

            var title = form.dataset.confirmTitle || 'Xác nhận thao tác';
            var icon = form.dataset.confirmIcon || 'warning';
            var btnCancel = form.dataset.confirmCancel || 'Hủy';
            var btnOk = form.dataset.confirmOk || 'Xác nhận';

            showConfirmModal({
                title: title,
                message: msg,
                icon: icon,
                onConfirm: function () {
                    closeConfirmModal();
                    // Chống bấm đúp
                    var submitBtn = form.querySelector('[type="submit"]');
                    if (submitBtn) {
                        submitBtn.disabled = true;
                        submitBtn.dataset.originalText = submitBtn.textContent;
                        submitBtn.textContent = 'Đang xử lý...';
                    }
                    form.submit();
                },
                onCancel: function () {
                    closeConfirmModal();
                }
            });
        });

        // Modal HTML
        var modalEl = document.getElementById('confirm-modal');
        if (!modalEl) {
            modalEl = document.createElement('dialog');
            modalEl.id = 'confirm-modal';
            modalEl.className = 'modal-overlay';
            modalEl.innerHTML =
                '<div class="modal-panel confirm-modal">' +
                '  <div class="confirm-modal-icon" id="cm-icon"></div>' +
                '  <h3 class="confirm-modal-title" id="cm-title"></h3>' +
                '  <p class="confirm-modal-text" id="cm-text"></p>' +
                '  <div class="confirm-modal-actions">' +
                '    <button type="button" class="btn btn-secondary" id="cm-cancel"></button>' +
                '    <button type="button" class="btn btn-primary" id="cm-ok"></button>' +
                '  </div>' +
                '</div>';
            document.body.appendChild(modalEl);
        }

        var currentCallback = null;
        var cmIcon = document.getElementById('cm-icon');
        var cmTitle = document.getElementById('cm-title');
        var cmText = document.getElementById('cm-text');
        var cmCancel = document.getElementById('cm-cancel');
        var cmOk = document.getElementById('cm-ok');
        var cmPanel = modalEl.querySelector('.modal-panel');

        window.showConfirmModal = function (opts) {
            currentCallback = opts;
            cmIcon.className = 'confirm-modal-icon ' + (opts.icon || 'warning');
            cmIcon.textContent = opts.icon === 'danger' ? '⚠' : opts.icon === 'success' ? '✓' : opts.icon === 'info' ? 'ℹ' : '?';
            cmTitle.textContent = opts.title || 'Xác nhận';
            cmText.textContent = opts.message || '';
            cmCancel.textContent = opts.onCancel ? (opts.btnCancel || 'Hủy') : '';
            cmOk.textContent = opts.btnOk || 'Xác nhận';

            // Màu nút OK theo icon
            cmOk.className = 'btn ' + (opts.icon === 'danger' ? 'btn-danger' : opts.icon === 'success' ? 'btn-success' : 'btn-primary');

            cmCancel.style.display = opts.onCancel ? '' : 'none';

            modalEl.showModal();
            cmOk.focus();
        };

        window.closeConfirmModal = function () {
            modalEl.close();
            currentCallback = null;
        };

        cmOk.addEventListener('click', function () {
            if (currentCallback && currentCallback.onConfirm) {
                currentCallback.onConfirm();
            }
        });

        cmCancel.addEventListener('click', function () {
            if (currentCallback && currentCallback.onCancel) {
                currentCallback.onCancel();
            } else {
                closeConfirmModal();
            }
        });

        modalEl.addEventListener('click', function (e) {
            if (e.target === modalEl) {
                if (currentCallback && currentCallback.onCancel) {
                    currentCallback.onCancel();
                } else {
                    closeConfirmModal();
                }
            }
        });

        modalEl.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                e.preventDefault();
                if (currentCallback && currentCallback.onCancel) {
                    currentCallback.onCancel();
                } else {
                    closeConfirmModal();
                }
            }
        });
    }

    // ========== TOGGLE TRƯỜNG THEO PERIOD TYPE ==========
    window.htqlt = window.htqlt || {};
    window.htqlt.togglePeriodTypeFields = function () {
        var sel = document.getElementById('periodType') || document.querySelector('[name="periodType"]');
        var reviewDeadlineRow = document.getElementById('reviewDeadlineRow');
        var councilDateRow = document.getElementById('councilDateRow');

        function update() {
            if (!sel) return;
            var v = sel.value;
            if (reviewDeadlineRow) {
                reviewDeadlineRow.style.display = (v === 'TLCN' || v === 'KLTN') ? '' : 'none';
                var inp = reviewDeadlineRow.querySelector('input');
                if (inp && (v !== 'TLCN' && v !== 'KLTN')) inp.value = '';
            }
            if (councilDateRow) {
                councilDateRow.style.display = (v === 'KLTN') ? '' : 'none';
                var inp = councilDateRow.querySelector('input');
                if (inp && v !== 'KLTN') inp.value = '';
            }
        }

        if (sel) {
            sel.addEventListener('change', update);
            update();
        }
    };

    // ========== HIỆN/ẨN MẬT KHẨU ==========
    function initPasswordToggle() {
        document.querySelectorAll('.password-toggle').forEach(function (btn) {
            var targetId = btn.dataset.target;
            var target = targetId ? document.getElementById(targetId) : btn.previousElementSibling;
            if (!target) return;

            btn.setAttribute('type', 'button');
            btn.setAttribute('aria-label', 'Hiện mật khẩu');
            btn.style.cssText = 'background:none;border:none;cursor:pointer;padding:4px 8px;font-size:13px;color:var(--muted);';

            btn.addEventListener('click', function () {
                var isPassword = target.type === 'password';
                target.type = isPassword ? 'text' : 'password';
                btn.textContent = isPassword ? 'Ẩn' : 'Hiện';
                btn.setAttribute('aria-label', isPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
            });
        });
    }

    // ========== CHỐNG BẤM ĐÚP ==========
    function initDoubleClickGuard() {
        document.querySelectorAll('form').forEach(function (form) {
            form.addEventListener('submit', function (e) {
                var submitBtn = form.querySelector('[type="submit"]:not(.no-guard)');
                if (!submitBtn || submitBtn.disabled) {
                    e.preventDefault();
                    return;
                }
                submitBtn.disabled = true;
                var original = submitBtn.textContent;
                submitBtn.dataset.originalText = original;
                submitBtn.textContent = 'Đang xử lý...';
                setTimeout(function () {
                    if (submitBtn.disabled) {
                        submitBtn.disabled = false;
                        submitBtn.textContent = original;
                    }
                }, 15000);
            });
        });
    }

    // ========== TÌM NHANH SV TRONG CHỌN THÀNH VIÊN ==========
    function initMemberSearch() {
        var searchInput = document.getElementById('member-search');
        var listContainer = document.getElementById('member-list');
        if (!searchInput || !listContainer) return;

        searchInput.addEventListener('input', function () {
            var keyword = searchInput.value.trim().toLowerCase();
            var items = listContainer.querySelectorAll('.member-item');
            items.forEach(function (item) {
                var text = (item.dataset.searchText || '').toLowerCase();
                item.style.display = text.includes(keyword) ? '' : 'none';
            });
        });
    }

    // ========== TÌM KIẾM / LỌC / PHÂN TRANG PHÍA TRÌNH DUYỆT ==========
    function initTableFilter() {
        document.querySelectorAll('[data-table-filter]').forEach(function (tableWrap) {
            var table = tableWrap.querySelector('table');
            if (!table) return;

            var keywordInput = tableWrap.querySelector('[data-filter-keyword]');
            var statusSelect = tableWrap.querySelector('[data-filter-status]');
            var periodSelect = tableWrap.querySelector('[data-filter-period]');
            var rows = Array.from(table.querySelectorAll('tbody tr[data-row]'));
            var emptyRow = table.querySelector('.empty-row');

            // Lưu trạng thái
            try {
                var savedKeyword = sessionStorage.getItem('filter_keyword');
                var savedStatus = sessionStorage.getItem('filter_status');
                var savedPerPage = sessionStorage.getItem('filter_perpage') || '20';
                var savedPage = parseInt(sessionStorage.getItem('filter_page') || '1', 10);
                if (keywordInput && savedKeyword) keywordInput.value = savedKeyword;
                if (statusSelect && savedStatus) statusSelect.value = savedStatus;
            } catch (e) { /* ignore */ }

            var perPage = parseInt(tableWrap.dataset.perPage || '20', 10);
            var currentPage = 1;

            function applyFilters() {
                var keyword = keywordInput ? keywordInput.value.trim().toLowerCase() : '';
                var status = statusSelect ? statusSelect.value : '';
                var period = periodSelect ? periodSelect.value : '';

                if (keywordInput) try { sessionStorage.setItem('filter_keyword', keywordInput.value); } catch (e) {}
                if (statusSelect) try { sessionStorage.setItem('filter_status', status); } catch (e) {}

                var visible = rows.filter(function (row) {
                    var text = (row.dataset.search || '').toLowerCase();
                    var rowStatus = row.dataset.status || '';
                    var rowPeriod = row.dataset.period || '';
                    return text.includes(keyword)
                        && (!status || rowStatus === status)
                        && (!period || rowPeriod === period);
                });

                // Pagination
                var totalPages = Math.max(1, Math.ceil(visible.length / perPage));
                if (currentPage > totalPages) currentPage = totalPages;
                var start = (currentPage - 1) * perPage;
                var pageItems = visible.slice(start, start + perPage);

                rows.forEach(function (r) { r.style.display = 'none'; });
                pageItems.forEach(function (r) { r.style.display = ''; });

                if (emptyRow) {
                    emptyRow.style.display = visible.length === 0 ? '' : 'none';
                }

                // Cập nhật pagination UI
                updatePagination(visible.length, totalPages);
            }

            function updatePagination(total, pages) {
                var bar = tableWrap.querySelector('.pagination-bar');
                if (!bar) return;

                var info = bar.querySelector('.pagination-info');
                var list = bar.querySelector('.pagination-list');
                if (!list) return;

                if (info) {
                    info.textContent = 'Hiển thị ' + total + ' kết quả';
                }

                var start = (currentPage - 1) * perPage + 1;
                var end = Math.min(currentPage * perPage, total);
                if (info && total > 0) {
                    info.textContent = 'Hiển thị ' + start + '–' + end + ' / ' + total + ' kết quả';
                }

                list.innerHTML = '';
                if (pages <= 1) return;

                var prev = document.createElement('li');
                prev.innerHTML = '<a href="#">‹</a>';
                prev.classList.toggle('disabled', currentPage === 1);
                prev.querySelector('a').addEventListener('click', function (e) {
                    e.preventDefault();
                    if (currentPage > 1) { currentPage--; applyFilters(); }
                });
                list.appendChild(prev);

                var range = getPageRange(currentPage, pages);
                range.forEach(function (p) {
                    if (p === '...') {
                        var ell = document.createElement('li');
                        ell.innerHTML = '<span>…</span>';
                        list.appendChild(ell);
                    } else {
                        var li = document.createElement('li');
                        li.innerHTML = '<a href="#">' + p + '</a>';
                        if (p === currentPage) li.classList.add('active');
                        li.querySelector('a').addEventListener('click', function (e) {
                            e.preventDefault();
                            currentPage = p;
                            try { sessionStorage.setItem('filter_page', currentPage); } catch (e) {}
                            applyFilters();
                        });
                        list.appendChild(li);
                    }
                });

                var next = document.createElement('li');
                next.innerHTML = '<a href="#">›</a>';
                next.classList.toggle('disabled', currentPage === pages);
                next.querySelector('a').addEventListener('click', function (e) {
                    e.preventDefault();
                    if (currentPage < pages) { currentPage++; applyFilters(); }
                });
                list.appendChild(next);
            }

            function getPageRange(current, total) {
                var pages = [];
                if (total <= 7) {
                    for (var i = 1; i <= total; i++) pages.push(i);
                } else {
                    pages.push(1);
                    if (current > 3) pages.push('...');
                    for (var i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) pages.push(i);
                    if (current < total - 2) pages.push('...');
                    pages.push(total);
                }
                return pages;
            }

            if (keywordInput) keywordInput.addEventListener('input', function () { currentPage = 1; applyFilters(); });
            if (statusSelect) statusSelect.addEventListener('change', function () { currentPage = 1; applyFilters(); });
            if (periodSelect) periodSelect.addEventListener('change', function () { currentPage = 1; applyFilters(); });

            // Per-page select
            var perPageSelect = tableWrap.querySelector('[data-filter-perpage]');
            if (perPageSelect) {
                perPageSelect.addEventListener('change', function () {
                    perPage = parseInt(perPageSelect.value, 10);
                    currentPage = 1;
                    applyFilters();
                });
            }

            applyFilters();
        });
    }

    // ========== INIT ALL ==========
    function init() {
        initSidebar();
        initFlash();
        initConfirmModal();
        initPasswordToggle();
        initDoubleClickGuard();
        initMemberSearch();
        initTableFilter();
        window.htqlt.togglePeriodTypeFields();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

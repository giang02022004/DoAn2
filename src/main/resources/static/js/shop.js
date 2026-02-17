// Global variables to store current filter state
let currentPage = 0;
let currentHangId = null;
let currentLoai = null;
let currentMaxPrice = 0;
let currentKeyword = '';
let currentSort = 'id_desc';

$(document).ready(function() {
    // Initial load handled by server-side rendering, 
    // but we need to bind events to existing elements if any.
    // Actually, for consistency, we might want to trigger a load or just simply bind events.
    // For now, let's just bind the click events for the sidebar.
    
    // Bind click events to brand links
    $('.fruite-categorie a').on('click', function(e) {
        e.preventDefault();
        
        // Extract params from href if present, or data attributes
        // Let's parse the URL in the href because that's what we generated in Thymeleaf
        const href = $(this).attr('href');
        const urlParams = new URLSearchParams(href.split('?')[1]);
        
        if (urlParams.has('hangId')) {
            currentHangId = urlParams.get('hangId');
        } else {
            currentHangId = null;
        }
        
        if (urlParams.has('loai')) {
            currentLoai = urlParams.get('loai');
        } else {
            currentLoai = null;
        }
        
        currentPage = 0; // Reset to page 0 on filter change
        
        // Update UI active state
        $('.fruite-categorie a').removeClass('active text-warning fw-bold');
        $(this).addClass('active'); // Our new CSS uses .active
        
        loadProducts();
    });

    // Bind event to price slider (Change - trigger filter)
    $('#rangeInput').on('change', function() {
        currentMaxPrice = $(this).val();
        currentPage = 0; // Reset page
        loadProducts();
    });

    // Bind event for live display update (Input - trigger visual update)
    $('#rangeInput').on('input', function() {
        const value = $(this).val();
        const formattedValue = new Intl.NumberFormat('vi-VN').format(value) + ' VNĐ';
        $('#amount').text(formattedValue);
    });
    
    // Initialize display if val is 0 or preset
    const initialVal = $('#rangeInput').val();
    if (initialVal == 0) {
        $('#amount').text("0 VNĐ (Không lọc)");
    } else {
        $('#amount').text(new Intl.NumberFormat('vi-VN').format(initialVal) + ' VNĐ');
    }

    // --- Search Logic ---
    function performSearch(keyword) {
        currentKeyword = keyword;
        currentPage = 0;
        loadProducts();
        // Close modal if open
        $('#searchModal').modal('hide'); 
    }

    // Sidebar Search
    $('#searchBtn').on('click', function() {
        performSearch($('#searchInput').val());
    });

    $('#searchInput').on('keypress', function(e) {
        if (e.which == 13) {
            performSearch($(this).val());
            return false;
        }
    });

    // Modal Search
    $('#modalSearchBtn').on('click', function() {
        performSearch($('#modalSearchInput').val());
    });

    $('#modalSearchInput').on('keypress', function(e) {
        if (e.which == 13) {
            performSearch($(this).val());
            return false;
        }
    });

    // Sort Change
    $('#sortItem').on('change', function() {
        currentSort = $(this).val();
        currentPage = 0;
        loadProducts();
    });

    // Reset Filters
    $('#resetFiltersBtn').on('click', function() {
        // Reset variables
        currentHangId = null;
        currentLoai = null;
        currentMaxPrice = 0;
        currentKeyword = '';
        currentSort = 'id_desc';
        currentPage = 0;

        // Reset UI elements
        $('#searchInput').val('');
        $('#modalSearchInput').val('');
        $('#rangeInput').val(0);
        $('#amount').text("0 VNĐ (Không lọc)");
        $('#sortItem').val('id_desc');
        $('.fruite-categorie a').removeClass('active text-warning fw-bold');
        
        loadProducts();
    });
});

/**
 * Hàm tải danh sách sản phẩm từ API
 * Sử dụng các biến toàn cục: currentPage, currentHangId, currentLoai, currentMaxPrice
 */
function loadProducts() {
    // Show spinner or opacity
    $('#product-list').css('opacity', '0.5');

    $.ajax({
        url: '/api/shop/products',
        type: 'GET',
        data: {
            page: currentPage,
            hangId: currentHangId,
            loai: currentLoai,
            maxPrice: currentMaxPrice,
            keyword: currentKeyword,
            sort: currentSort
        },
        success: function(response) {
            renderProducts(response.content);
            renderPagination(response);
            $('#product-list').css('opacity', '1');
            
            // Scroll to top of product list
            $('html, body').animate({
                scrollTop: $(".fruite").offset().top
            }, 500);
        },
        error: function(xhr, status, error) {
            console.error("Error loading products:", error);
            $('#product-list').css('opacity', '1');
        }
    });
}

/**
 * Hàm hiển thị danh sách sản phẩm lên giao diện
 * @param {Array} products - Danh sách sản phẩm trả về từ API
 */
function renderProducts(products) {
    const container = $('#product-list');
    container.empty();

    if (products.length === 0) {
        container.append(`
            <div class="col-12 text-center py-5">
                <i class="fa fa-search fa-3x text-secondary mb-3"></i>
                <h4 class="text-secondary">Không tìm thấy sản phẩm nào!</h4>
                <p class="text-muted">Hãy thử thay đổi tiêu chí tìm kiếm hoặc làm mới bộ lọc.</p>
            </div>
        `);
        return;
    }

    products.forEach(sp => {
        const formattedPrice = new Intl.NumberFormat('vi-VN').format(sp.gia);
        
        const html = `
            <div class="col-md-6 col-lg-6 col-xl-4">
                <div class="rounded position-relative fruite-item">
                    <div class="fruite-img">
                        <img src="/img/${sp.hinhAnh}" class="img-fluid w-100 rounded-top" alt="" style="height: 250px; object-fit: cover;">
                    </div>
                    <div class="text-white bg-secondary px-3 py-1 rounded position-absolute" style="top: 10px; left: 10px;">${sp.loaiSanPham.tenLoai}</div>
                    <div class="p-4 border border-secondary border-top-0 rounded-bottom">
                        <h4>${sp.tenSanPham}</h4>
                        <p>${sp.moTaNgan}</p>
                        <div class="d-flex justify-content-between flex-lg-wrap">
                            <p class="text-dark fs-5 fw-bold mb-0">${formattedPrice} VNĐ</p>
                            <a href="/shop-detail/${sp.id}" class="btn border border-secondary rounded-pill px-3 text-primary"><i class="fa fa-shopping-bag me-2 text-primary"></i> Mua ngay</a>
                        </div>
                    </div>
                </div>
            </div>
        `;
        container.append(html);
    });
}

/**
 * Hàm hiển thị phân trang
 * @param {Object} pageData - Dữ liệu phân trang trả về từ API
 */
function renderPagination(pageData) {
    const paginationContainer = $('#pagination');
    paginationContainer.empty();

    if (pageData.totalPages <= 1) return;

    let html = '';
    
    // Previous button
    if (pageData.number > 0) {
        html += `<a href="#" onclick="changePage(${pageData.number - 1}); return false;" class="rounded">&laquo;</a>`;
    }

    // Page numbers
    for (let i = 0; i < pageData.totalPages; i++) {
        const activeClass = (i === pageData.number) ? 'active' : '';
        html += `<a href="#" onclick="changePage(${i}); return false;" class="${activeClass} rounded">${i + 1}</a>`;
    }

    // Next button
    if (pageData.number < pageData.totalPages - 1) {
        html += `<a href="#" onclick="changePage(${pageData.number + 1}); return false;" class="rounded">&raquo;</a>`;
    }

    paginationContainer.append(html);
}

/**
 * Hàm thay đổi trang hiện tại và tải lại sản phẩm
 * @param {number} page - Số trang cần chuyển đến (0-indexed)
 */
function changePage(page) {
    currentPage = page;
    loadProducts();
}

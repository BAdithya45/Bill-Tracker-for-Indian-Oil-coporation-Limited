// Indian Oil Bill Tracker - Frontend JavaScript
class IOCBillTracker {
    constructor() {
        this.currentUser = null;
        this.bills = [];
        this.filteredBills = [];
        this.networks = [];
        this.vendors = [];
        this.quarters = [];
        this.locations = [];
        this.charts = {};
        this.networkConfig = {}; // Store network-specific vendor and quarter configurations
        this.baseUrl = this.getBaseUrl(); // Detect correct base URL for API calls
        this.init();
    }

    // Get the correct base URL for API calls (works with both embedded and external Tomcat)
    getBaseUrl() {
        const path = window.location.pathname;
        const hostname = window.location.hostname;
        const port = window.location.port;
        
        console.log('=== BASE URL DETECTION ===');
        console.log('Current URL:', window.location.href);
        console.log('Current path:', path);
        console.log('Hostname:', hostname);
        console.log('Port:', port);
        
        // More robust detection for external Tomcat
        if (path.includes('/bill') || path.startsWith('/bill')) {
            console.log('✅ External Tomcat detected, using base URL: /bill');
            return '/bill';
        }
        console.log('✅ Embedded Tomcat detected, using base URL: (empty)');
        return '';
    }

    // Helper function to make API calls with proper base URL and error handling
    async apiCall(endpoint, options = {}) {
        const fullUrl = `${this.baseUrl}${endpoint}`;
        console.log('🔄 API Call:', fullUrl);
        
        // Ensure credentials are included for session handling
        const fetchOptions = {
            credentials: 'include',
            ...options
        };
        
        try {
            const response = await fetch(fullUrl, fetchOptions);
            console.log('📡 Response status:', response.status, response.statusText);
            console.log('📡 Response headers:', Object.fromEntries(response.headers));
            
            // Handle 401 Unauthorized - session expired or not authenticated
            if (response.status === 401) {
                console.log('🔐 401 Unauthorized - Session expired or not authenticated');
                // For auth endpoints, let the calling method handle the 401
                if (endpoint.startsWith('/api/auth/')) {
                    console.log('🔐 Auth endpoint - letting caller handle 401');
                } else {
                    console.log('🔐 Non-auth endpoint - redirecting to login');
                    // For other endpoints, redirect to login
                    this.currentUser = null;
                    this.showLoginSection();
                }
                throw new Error('Authentication required');
            }
            
            // Check if response is JSON or HTML
            const contentType = response.headers.get('content-type');
            console.log('📄 Content-Type:', contentType);
            
            if (contentType && contentType.includes('application/json')) {
                const data = await response.json();
                console.log('✅ JSON Response:', data);
                return data;
            } else {
                // If not JSON, it's likely an HTML error page
                const htmlText = await response.text();
                console.error('❌ Received HTML instead of JSON:');
                console.error('HTML Preview:', htmlText.substring(0, 200) + '...');
                throw new Error(`Server error: ${response.status} ${response.statusText}. Expected JSON but got HTML. Check if API endpoint '${fullUrl}' is available.`);
            }
        } catch (error) {
            console.error('❌ API call error:', error);
            throw error;
        }
    }

    async init() {
        console.log('🔄 Initializing app - checking authentication first');
        this.setupEventListeners();
        
        // Check if user is already authenticated before showing login
        try {
            await this.checkAuthentication();
            console.log('✅ User is authenticated, showing main content');
        } catch (error) {
            console.log('❌ User not authenticated, showing login page');
            this.showLoginSection();
        }
        
        console.log('✅ App initialization complete');
    }

    setupEventListeners() {
        // Login/Register form switching
        document.getElementById('showRegister')?.addEventListener('click', () => {
            this.showRegisterForm();
        });

        document.getElementById('showLogin')?.addEventListener('click', () => {
            this.showLoginForm();
        });

        // Form submissions
        document.getElementById('loginFormElement')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleLogin();
        });

        document.getElementById('registerFormElement')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleRegister();
        });

        // Logout
        document.getElementById('logoutBtn')?.addEventListener('click', () => {
            this.handleLogout();
        });

        // Tab switching
        document.querySelectorAll('[data-bs-toggle="tab"]').forEach(tab => {
            tab.addEventListener('shown.bs.tab', (e) => {
                const targetId = e.target.getAttribute('data-bs-target');
                this.handleTabSwitch(targetId);
            });
        });

        // Update form
        document.getElementById('updateBillForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveBill();
        });
        
        // Form reset handler
        document.getElementById('updateBillForm')?.addEventListener('reset', (e) => {
            setTimeout(() => {
                const saveButton = document.querySelector('#updateBillForm button[type="submit"]');
                if (saveButton) {
                    saveButton.innerHTML = '<i class="fas fa-save me-2"></i>Save Entry';
                    delete saveButton.dataset.editingSerialNo;
                }
                // Hide PDF info on form reset
                const currentPdfInfo = document.getElementById('currentPdfInfo');
                if (currentPdfInfo) {
                    currentPdfInfo.style.display = 'none';
                }
            }, 10);
        });

        // Filters
        document.getElementById('searchFilter')?.addEventListener('input', () => this.applyFilters());
        document.getElementById('yearFilter')?.addEventListener('change', () => this.applyFilters());
        document.getElementById('networkFilter')?.addEventListener('change', () => {
            this.updateVendorFilterForNetwork();
            this.applyFilters();
        });
        document.getElementById('vendorFilter')?.addEventListener('change', () => this.applyFilters());
        document.getElementById('quarterFilter')?.addEventListener('change', () => this.applyFilters());
        document.getElementById('locationFilter')?.addEventListener('change', () => this.applyFilters());
        document.getElementById('statusFilter')?.addEventListener('change', () => this.applyFilters());
        document.getElementById('glCodeFilter')?.addEventListener('change', () => this.applyFilters());
        document.getElementById('costCenterFilter')?.addEventListener('change', () => this.applyFilters());
        document.getElementById('commitItemFilter')?.addEventListener('change', () => this.applyFilters());
        document.getElementById('clearFilters')?.addEventListener('click', () => this.clearFilters());

        // Analytics filters - Add network dependency for vendor
        document.getElementById('analyticsNetworkFilter')?.addEventListener('change', () => {
            this.updateAnalyticsVendorFilterForNetwork();
            this.loadAnalytics(); // Auto-apply filter when network changes
        });

        // Analytics filters - Add individual filter change listeners
        document.getElementById('analyticsYearFilter')?.addEventListener('change', () => this.loadAnalytics());
        document.getElementById('analyticsQuarterFilter')?.addEventListener('change', () => this.loadAnalytics());
        document.getElementById('analyticsVendorFilter')?.addEventListener('change', () => this.loadAnalytics());
        document.getElementById('analyticsGlCodeFilter')?.addEventListener('change', () => this.loadAnalytics());
        document.getElementById('analyticsCostCenterFilter')?.addEventListener('change', () => this.loadAnalytics());
        document.getElementById('analyticsCommitItemFilter')?.addEventListener('change', () => this.loadAnalytics());

        // Analytics filters
        document.getElementById('applyAnalyticsFilters')?.addEventListener('click', () => {
            this.loadAnalytics();
        });
        
        document.getElementById('clearAnalyticsFilters')?.addEventListener('click', () => {
            // Clear all filter selections
            document.getElementById('analyticsYearFilter').value = '';
            document.getElementById('analyticsQuarterFilter').value = '';
            document.getElementById('analyticsNetworkFilter').value = '';
            document.getElementById('analyticsVendorFilter').value = '';
            document.getElementById('analyticsGlCodeFilter').value = '';
            document.getElementById('analyticsCostCenterFilter').value = '';
            document.getElementById('analyticsCommitItemFilter').value = '';
            this.loadAnalytics();
        });

        // Export
        document.getElementById('exportBtn')?.addEventListener('click', () => this.exportData());

        // Network management
        document.getElementById('addNetworkForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.addNetworkWithConfig();
        });
        document.getElementById('addVendorField')?.addEventListener('click', () => this.addVendorField());
        document.getElementById('quarterCount')?.addEventListener('change', (e) => this.generateQuarterInputs(e.target.value));

        // Network input enter keys
        document.getElementById('newNetworkName')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.addNetwork();
        });
        document.getElementById('newVendorName')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.addVendor();
        });
        document.getElementById('newQuarterName')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.addQuarter();
        });

        // Quarter selection handler for auto-filling billing period
        document.getElementById('updateQuarter')?.addEventListener('change', (e) => {
            this.updateBillingPeriod(e.target.value);
        });

        // Date selection handlers for auto-updating billing period
        document.getElementById('updateFromDate')?.addEventListener('change', () => {
            this.updateBillingPeriodFromDates();
        });

        document.getElementById('updateToDate')?.addEventListener('change', () => {
            this.updateBillingPeriodFromDates();
        });

        // Network selection handler for updating vendors
        document.getElementById('updateNetwork')?.addEventListener('change', (e) => {
            const networkValue = e.target.value;
            this.updateVendorDropdown(networkValue);
            this.updateQuarterDropdown(networkValue);
            
            // Clear dependent fields when network changes
            if (networkValue) {
                document.getElementById('updateVendor').value = '';
                document.getElementById('updateQuarter').value = '';
                document.getElementById('updateBillingPeriod').value = '';
            }
        });

        // Tax calculation handlers
        document.getElementById('updateBillWithTax')?.addEventListener('input', (e) => {
            this.calculateBillWithoutTax(e.target.value);
        });

        document.getElementById('updateBillWithoutTax')?.addEventListener('input', (e) => {
            this.calculateBillWithTax(e.target.value);
        });
    }

    showRegisterForm() {
        document.getElementById('loginForm').style.display = 'none';
        document.getElementById('registerForm').style.display = 'block';
    }

    showLoginForm() {
        const loginSection = document.getElementById('loginSection');
        const mainContent = document.getElementById('mainContent');
        
        loginSection.classList.remove('hide');
        loginSection.style.display = 'flex';
        mainContent.classList.remove('show');
        mainContent.style.display = 'none';
        document.body.classList.remove('logged-in');
        
        document.getElementById('registerForm').style.display = 'none';
        document.getElementById('loginForm').style.display = 'block';
    }

    async handleLogin() {
        const username = document.getElementById('loginUsername').value;
        const password = document.getElementById('loginPassword').value;

        if (!username || !password) {
            this.showAlert('Please enter both username and password', 'danger');
            return;
        }

        try {
            // Try main login endpoint first
            let data;
           
            try {
                data = await this.apiCall('/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ username, password })
                });
            } catch (error) {
                console.log('Main login endpoint failed, trying alternative...', error.message);
                // If main endpoint fails, try alternative endpoint
                data = await this.apiCall('/api/auth/login-alt', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ username, password })
                });
            }
            
            if (data.success) {
                this.currentUser = data.user;
                this.showMainContent();
                await this.loadInitialData();
                
                // Clear form
                document.getElementById('loginFormElement').reset();
            } else {
                this.showAlert('Login failed: ' + (data.message || 'Unknown error'), 'danger');
            }
        } catch (error) {
            console.error('Login error:', error);
            this.showAlert('Login failed: ' + error.message, 'danger');
        }
    }

    async handleRegister() {
        const username = document.getElementById('registerUsername').value;
        const password = document.getElementById('registerPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (password !== confirmPassword) {
            this.showAlert('Passwords do not match', 'danger');
            return;
        }

        try {
            const data = await this.apiCall('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password })
            });
            
            if (data.success) {
                this.showAlert('Registration successful! Please login.', 'success');
                this.showLoginForm();
                document.getElementById('registerFormElement').reset();
            } else {
                this.showAlert('Registration failed: ' + data.message, 'danger');
            }
        } catch (error) {
            this.showAlert('Registration failed: ' + error.message, 'danger');
        }
    }

    async handleLogout() {
        try {
            await this.apiCall('/api/auth/logout', {
                method: 'POST'
            });
            
            this.currentUser = null;
            this.showLoginSection();
        } catch (error) {
            console.error('Logout error:', error);
            this.showLoginSection(); // Force logout even if API call fails
        }
    }

    async checkAuthentication() {
        try {
            console.log('🔐 Checking authentication status...');
            console.log('🔐 Current cookies:', document.cookie);
            
            const user = await this.apiCall('/api/auth/me');
            console.log('🔐 Authentication successful:', user);
            
            this.currentUser = user;
            this.showMainContent();
            await this.loadInitialData();
        } catch (error) {
            console.log('🔐 Authentication failed:', error.message);
            console.log('🔐 Available cookies:', document.cookie);
            this.showLoginSection();
        }
    }

    showLoginSection() {
        // Force logout API call to clear server session
        fetch('/api/auth/logout', { method: 'POST', credentials: 'include' }).catch(() => {});
        
        const loginSection = document.getElementById('loginSection');
        const mainContent = document.getElementById('mainContent');
        
        loginSection.classList.remove('hide');
        loginSection.style.display = 'flex';
        
        mainContent.classList.remove('show');
        mainContent.style.display = 'none';
    }

    showMainContent() {
        const loginSection = document.getElementById('loginSection');
        const mainContent = document.getElementById('mainContent');
        
        loginSection.classList.add('hide');
        loginSection.style.display = 'none';
        
        mainContent.classList.add('show');
        mainContent.style.display = 'block';
        document.body.classList.add('logged-in');
        
        if (this.currentUser) {
            document.getElementById('currentUser').textContent = this.currentUser.username;
            this.setupRoleBasedAccess();
        }
        
        // Redirect to dashboard - activate View tab by default
        const viewTab = document.getElementById('view-tab');
        if (viewTab) {
            const bootstrap_tab = new bootstrap.Tab(viewTab);
            bootstrap_tab.show();
        }
    }

    setupRoleBasedAccess() {
        const isAdmin = this.currentUser && this.currentUser.isAdmin;
        
        // Show/hide Network tab based on admin role
        const networkTab = document.getElementById('network-tab');
        if (networkTab) {
            networkTab.style.display = isAdmin ? 'block' : 'none';
        }
        
        // Show/hide Admin tab based on admin role
        const adminTab = document.getElementById('admin-tab');
        if (adminTab) {
            adminTab.style.display = isAdmin ? 'block' : 'none';
        }
        
        // Load admin functions if user is admin
        if (isAdmin) {
            this.loadAdminFunctions();
        }
    }

    async loadAdminFunctions() {
        // Load users list for admin panel
        await this.loadUsersList();
        
        // Setup admin event listeners
        this.setupAdminEventListeners();
    }

    setupAdminEventListeners() {
        // Add User Form
        document.getElementById('addUserForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.addUser();
        });
        
        // Change My Password Form
        document.getElementById('changeMyPasswordForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.changeMyPassword();
        });
        
        // Reset User Password Form
        document.getElementById('resetUserPasswordForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.resetUserPassword();
        });
    }

    async loadInitialData() {
        await Promise.all([
            this.loadBills(),
            this.loadNetworkData(),
            this.loadLocations()
        ]);
        this.populateFilters();
        this.applyFilters();
        // Debug logging
        this.testEditAndDownload();
    }

    handleTabSwitch(targetId) {
        switch (targetId) {
            case '#view-panel':
                this.applyFilters();
                break;
            case '#update-panel':
                this.populateUpdateForm();
                break;
            case '#analytics-panel':
                this.loadAnalytics();
                break;
            case '#network-panel':
                this.loadNetworkManagement();
                break;
        }
    }

    async loadBills() {
        try {
            console.log('🔄 Starting loadBills()...');
            const response = await this.apiCall('/api/bills');
            console.log('📊 Bills API response:', response); // Debug log
            console.log('📊 Response type:', typeof response, 'Is array:', Array.isArray(response));
            
            // Check if response is an error object or an array
            if (response && response.error) {
                console.log('❌ Bills API returned error:', response.error);
                this.bills = [];
                this.filteredBills = [];
                this.showAlert('Could not load bills: ' + response.error, 'warning');
                return;
            }
            
            this.bills = Array.isArray(response) ? response : [];
            this.filteredBills = [...this.bills];
            console.log('✅ Loaded bills:', this.bills.length, 'bills'); // Debug log
            console.log('📊 First few bills:', this.bills.slice(0, 3)); // Show first 3 bills
            
            // Debug: Log bills with PDF paths
            const billsWithPdf = this.bills.filter(bill => bill.pdfFilePath);
            console.log('📎 Bills with PDF:', billsWithPdf.length, billsWithPdf); // Debug log
            
            // Force update the view if we're on the View tab
            if (document.getElementById('view-tab').classList.contains('active')) {
                console.log('🔄 Refreshing view table...');
                this.renderViewTable();
                this.updateViewSummary();
            }
        } catch (error) {
            console.error('❌ Error loading bills:', error);
            this.showAlert('Error loading bills: ' + error.message, 'danger');
            // Ensure bills is always an array even on error
            this.bills = [];
            this.filteredBills = [];
        }
    }

    async loadNetworkData() {
        try {
            const config = await this.apiCall('/api/bills/config');
            
            // Check if response is an error object
            if (config && config.error) {
                console.log('Network data API returned error:', config.error);
                this.networks = [];
                this.vendors = [];
                this.quarters = [];
                this.networkConfig = {};
                return;
            }
            
            this.networks = config.networks || [];
            this.vendors = config.vendors || [];
            this.quarters = config.quarters || [];
            this.networkConfig = config.networkConfig || {};
        } catch (error) {
            console.error('Error loading network data:', error);
            this.networks = [];
            this.vendors = [];
            this.quarters = [];
            this.networkConfig = {};
        }
    }

    async loadLocations() {
        try {
            const response = await this.apiCall('/api/bills/locations');
            
            // Check if response is an error object
            if (response && response.error) {
                console.log('Locations API returned error:', response.error);
                this.locations = [];
                return;
            }
            
            this.locations = Array.isArray(response) ? response : [];
        } catch (error) {
            console.error('Error loading locations:', error);
            this.locations = [];
        }
    }

    populateFilters() {
        // Ensure bills is an array before using map
        if (!Array.isArray(this.bills)) {
            console.log('Bills is not an array, initializing empty array');
            this.bills = [];
        }
        
        // Populate year filter
        const yearFilter = document.getElementById('yearFilter');
        if (yearFilter) {
            yearFilter.innerHTML = '<option value="">All Years</option>';
            const years = [...new Set(this.bills.map(b => {
                if (b.fromDate) {
                    return new Date(b.fromDate).getFullYear();
                }
                return null;
            }).filter(Boolean))].sort((a, b) => b - a);
            years.forEach(year => {
                const option = document.createElement('option');
                option.value = year;
                option.textContent = year;
                yearFilter.appendChild(option);
            });
        }

        // Populate network filter from bills data and networkConfig
        const networkFilter = document.getElementById('networkFilter');
        networkFilter.innerHTML = '<option value="">All Networks</option>';
        
        const networksFromBills = [...new Set(this.bills.map(b => b.network).filter(Boolean))];
        const networksFromConfig = this.networkConfig ? Object.keys(this.networkConfig) : [];
        const allNetworks = [...new Set([...networksFromBills, ...networksFromConfig])];
        
        allNetworks.forEach(network => {
            const option = document.createElement('option');
            option.value = network;
            option.textContent = network;
            networkFilter.appendChild(option);
        });

        // Populate vendor filter from bills data and networkConfig
        const vendorFilter = document.getElementById('vendorFilter');
        vendorFilter.innerHTML = '<option value="">All Vendors</option>';
        
        const vendorsFromBills = [...new Set(this.bills.map(b => b.vendor).filter(Boolean))];
        const vendorsFromConfig = [];
        if (this.networkConfig) {
            Object.values(this.networkConfig).forEach(config => {
                if (config.vendors) {
                    vendorsFromConfig.push(...config.vendors);
                }
            });
        }
        const allVendors = [...new Set([...vendorsFromBills, ...vendorsFromConfig])];
        
        allVendors.forEach(vendor => {
            const option = document.createElement('option');
            option.value = vendor;
            option.textContent = vendor;
            vendorFilter.appendChild(option);
        });

        // Populate quarter filter using quarterString (preferred) or quarter from bills and networkConfig
        const quarterFilter = document.getElementById('quarterFilter');
        quarterFilter.innerHTML = '<option value="">All Quarters</option>';
        
        const quartersFromBills = [...new Set(this.bills.map(b => b.quarterString || b.quarter).filter(Boolean))];
        const quartersFromConfig = [];
        if (this.networkConfig) {
            Object.values(this.networkConfig).forEach(config => {
                if (config.quarters) {
                    quartersFromConfig.push(...config.quarters.map(q => q.name));
                }
            });
        }
        const allQuarters = [...new Set([...quartersFromBills, ...quartersFromConfig])];
        
        allQuarters.forEach(quarter => {
            const option = document.createElement('option');
            option.value = quarter;
            option.textContent = quarter;
            quarterFilter.appendChild(option);
        });

        // Populate location filter
        const locationFilter = document.getElementById('locationFilter');
        if (locationFilter) {
            locationFilter.innerHTML = '<option value="">All Locations</option>';
            [...new Set(this.bills.map(b => b.location).filter(Boolean))].forEach(location => {
                const option = document.createElement('option');
                option.value = location;
                option.textContent = location;
                locationFilter.appendChild(option);
            });
        }

        // Populate GL Code filter
        const glCodeFilter = document.getElementById('glCodeFilter');
        if (glCodeFilter) {
            glCodeFilter.innerHTML = '<option value="">All GL Codes</option>';
            [...new Set(this.bills.map(b => b.glCode).filter(Boolean))].forEach(glCode => {
                const option = document.createElement('option');
                option.value = glCode;
                option.textContent = glCode;
                glCodeFilter.appendChild(option);
            });
        }

        // Populate Cost Center filter
        const costCenterFilter = document.getElementById('costCenterFilter');
        if (costCenterFilter) {
            costCenterFilter.innerHTML = '<option value="">All Cost Centers</option>';
            [...new Set(this.bills.map(b => b.costCenter).filter(Boolean))].forEach(costCenter => {
                const option = document.createElement('option');
                option.value = costCenter;
                option.textContent = costCenter;
                costCenterFilter.appendChild(option);
            });
        }

        // Populate Commit Item filter
        const commitItemFilter = document.getElementById('commitItemFilter');
        if (commitItemFilter) {
            commitItemFilter.innerHTML = '<option value="">All Commit Items</option>';
            [...new Set(this.bills.map(b => b.commitItem).filter(Boolean))].forEach(commitItem => {
                const option = document.createElement('option');
                option.value = commitItem;
                option.textContent = commitItem;
                commitItemFilter.appendChild(option);
            });
        }
    }

    // Alias for populateFilters - used by network management to refresh View tab dropdowns
    populateViewFilters() {
        this.populateFilters();
    }

    applyFilters() {
        const searchTerm = document.getElementById('searchFilter')?.value.toLowerCase() || '';
        const yearFilter = document.getElementById('yearFilter')?.value || '';
        const networkFilter = document.getElementById('networkFilter')?.value || '';
        const vendorFilter = document.getElementById('vendorFilter')?.value || '';
        const quarterFilter = document.getElementById('quarterFilter')?.value || '';
        const locationFilter = document.getElementById('locationFilter')?.value || '';
        const statusFilter = document.getElementById('statusFilter')?.value || '';
        const glCodeFilter = document.getElementById('glCodeFilter')?.value || '';
        const costCenterFilter = document.getElementById('costCenterFilter')?.value || '';
        const commitItemFilter = document.getElementById('commitItemFilter')?.value || '';

        this.filteredBills = this.bills.filter(bill => {
            const matchesSearch = !searchTerm || 
                Object.values(bill).some(value => 
                    value && value.toString().toLowerCase().includes(searchTerm)
                );
            const matchesYear = !yearFilter || 
                (bill.fromDate && new Date(bill.fromDate).getFullYear().toString() === yearFilter);
            const matchesNetwork = !networkFilter || bill.network === networkFilter;
            const matchesVendor = !vendorFilter || bill.vendor === vendorFilter;
            const matchesQuarter = !quarterFilter || 
                (bill.quarterString === quarterFilter || bill.quarter === quarterFilter);
            const matchesLocation = !locationFilter || bill.location === locationFilter;
            const matchesStatus = !statusFilter || bill.status === statusFilter;
            const matchesGlCode = !glCodeFilter || bill.glCode === glCodeFilter;
            const matchesCostCenter = !costCenterFilter || bill.costCenter === costCenterFilter;
            const matchesCommitItem = !commitItemFilter || bill.commitItem === commitItemFilter;

            return matchesSearch && matchesYear && matchesNetwork && matchesVendor && 
                   matchesQuarter && matchesLocation && matchesStatus && matchesGlCode && 
                   matchesCostCenter && matchesCommitItem;
        });

        this.renderViewTable();
        this.updateViewSummary();
    }

    clearFilters() {
        document.getElementById('searchFilter').value = '';
        document.getElementById('yearFilter').value = '';
        document.getElementById('networkFilter').value = '';
        document.getElementById('vendorFilter').value = '';
        document.getElementById('quarterFilter').value = '';
        document.getElementById('locationFilter').value = '';
        document.getElementById('statusFilter').value = '';
        document.getElementById('glCodeFilter').value = '';
        document.getElementById('costCenterFilter').value = '';
        document.getElementById('commitItemFilter').value = '';
        this.applyFilters();
    }

    renderViewTable() {
        const tbody = document.getElementById('billsViewTableBody');
        if (!tbody) {
            console.error('❌ billsViewTableBody element not found!');
            return;
        }
        
        tbody.innerHTML = '';
        
        console.log('🔄 Rendering bills - Total:', this.bills.length, 'Filtered:', this.filteredBills.length);
        console.log('📊 Bills to render:', this.filteredBills);

        if (this.filteredBills.length === 0) {
            tbody.innerHTML = '<tr><td colspan="18" class="text-center text-muted">No bills found. Please check your filters or add new bills.</td></tr>';
            return;
        }

        this.filteredBills.forEach((bill, index) => {
            console.log(`📋 Rendering bill ${index + 1}:`, bill);
            console.log('Bill PDF path:', bill.pdfFilePath); // Debug log for PDF
            
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${bill.serialNo || ''}</td>
                <td>${bill.network || ''}</td>
                <td>${bill.vendor || ''}</td>
                <td>${bill.quarterString || bill.quarter || ''}</td>
                <td>${bill.location || ''}</td>
                <td>${bill.invoiceNumber || ''}</td>
                <td>\u20B9${(bill.billWithTax || 0).toLocaleString('en-IN', {minimumFractionDigits: 2})}</td>
                <td>\u20B9${(bill.billWithoutTax || 0).toLocaleString('en-IN', {minimumFractionDigits: 2})}</td>
                <td>${bill.ses1 || ''}</td>
                <td>${bill.ses2 || ''}</td>
                <td>${bill.billingPeriod || (bill.fromDate && bill.toDate ? `${bill.fromDate} to ${bill.toDate}` : '')}</td>
                <td><span class="badge ${bill.status === 'Completed' ? 'bg-success' : 'bg-warning'}">${bill.status || 'Pending'}</span></td>
                <td style="max-width: 150px; overflow: hidden; text-overflow: ellipsis;" title="${bill.remarks || ''}">${bill.remarks || ''}</td>
                <td>${bill.glCode || ''}</td>
                <td>${bill.commitItem || ''}</td>
                <td>${bill.costCenter || ''}</td>
                <td>
                    ${bill.pdfFilePath ? 
                        `<button class="btn btn-sm btn-outline-primary" onclick="tracker.downloadPdf('${bill.pdfFilePath.replace(/'/g, "\\'")}')" title="Download PDF">
                            <i class="fas fa-download"></i>
                        </button>` : 
                        `<span class="text-muted">No PDF</span>`
                    }
                </td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="tracker.editBill(${bill.serialNo}).catch(console.error)" title="Edit">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="tracker.deleteBill(${bill.serialNo})" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(row);
        });
    }

    updateViewSummary() {
        const totalBills = this.filteredBills.length;
        const totalAmount = this.filteredBills.reduce((sum, bill) => sum + (bill.billWithTax || 0), 0);

        document.getElementById('billsCount').textContent = `Total: ${totalBills} bills`;
        document.getElementById('totalAmount').textContent = 
            `Total Amount: \u20b9${totalAmount.toLocaleString('en-IN', {minimumFractionDigits: 2})}`;
    }

    populateUpdateForm() {
        // Populate networks only from networkConfig (user-added networks)
        const networkSelect = document.getElementById('updateNetwork');
        networkSelect.innerHTML = '<option value="">Select Network</option>';
        
        // Only add networks that have been configured through the UI
        if (this.networkConfig && Object.keys(this.networkConfig).length > 0) {
            Object.keys(this.networkConfig).forEach(network => {
                const option = document.createElement('option');
                option.value = network;
                option.textContent = network;
                networkSelect.appendChild(option);
            });
        }

        // Initialize vendor dropdown empty - will be populated when network is selected
        const vendorSelect = document.getElementById('updateVendor');
        vendorSelect.innerHTML = '<option value="">Select Network First</option>';

        // Initialize quarter dropdown empty - will be populated when network is selected
        const quarterSelect = document.getElementById('updateQuarter');
        quarterSelect.innerHTML = '<option value="">Select Network First</option>';
        
        // Populate locations
        const locationSelect = document.getElementById('updateLocation');
        locationSelect.innerHTML = '<option value="">Select Location</option>';
        this.locations.forEach(location => {
            const option = document.createElement('option');
            option.value = location;
            option.textContent = location;
            locationSelect.appendChild(option);
        });
    }

    updateBillingPeriod(quarterValue) {
        const billingPeriodInput = document.getElementById('updateBillingPeriod');
        if (!quarterValue || !billingPeriodInput) return;

        // Parse quarter and generate billing period
        let billingPeriod = '';
        if (quarterValue.includes('Q1')) {
            const year = quarterValue.split('-')[1] || new Date().getFullYear();
            billingPeriod = `April ${year} - June ${year}`;
        } else if (quarterValue.includes('Q2')) {
            const year = quarterValue.split('-')[1] || new Date().getFullYear();
            billingPeriod = `July ${year} - September ${year}`;
        } else if (quarterValue.includes('Q3')) {
            const year = quarterValue.split('-')[1] || new Date().getFullYear();
            billingPeriod = `October ${year} - December ${year}`;
        } else if (quarterValue.includes('Q4')) {
            const year = quarterValue.split('-')[1] || new Date().getFullYear();
            billingPeriod = `January ${parseInt(year) + 1} - March ${parseInt(year) + 1}`;
        } else {
            billingPeriod = quarterValue; // Use custom quarter name as-is
        }

        billingPeriodInput.value = billingPeriod;
    }

    updateBillingPeriodFromDates() {
        const fromDate = document.getElementById('updateFromDate')?.value;
        const toDate = document.getElementById('updateToDate')?.value;
        const billingPeriodInput = document.getElementById('updateBillingPeriod');
        
        if (!billingPeriodInput) return;

        if (fromDate && toDate) {
            // Format dates for display
            const fromDateObj = new Date(fromDate);
            const toDateObj = new Date(toDate);
            
            const fromDateFormatted = fromDateObj.toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
            });
            const toDateFormatted = toDateObj.toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
            });
            
            billingPeriodInput.value = `${fromDateFormatted} - ${toDateFormatted}`;
        } else if (fromDate) {
            const fromDateObj = new Date(fromDate);
            const fromDateFormatted = fromDateObj.toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
            });
            billingPeriodInput.value = `From: ${fromDateFormatted}`;
        } else if (toDate) {
            const toDateObj = new Date(toDate);
            const toDateFormatted = toDateObj.toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
            });
            billingPeriodInput.value = `To: ${toDateFormatted}`;
        } else {
            billingPeriodInput.value = '';
        }
    }

    async updateVendorDropdown(networkValue) {
        const vendorSelect = document.getElementById('updateVendor');
        if (!vendorSelect) return;

        if (!networkValue) {
            vendorSelect.innerHTML = '<option value="">Select Network First</option>';
            return;
        }

        console.log('Updating vendors for network:', networkValue);
        
        // Use networkConfig data instead of API call
        if (this.networkConfig && this.networkConfig[networkValue]) {
            const vendors = this.networkConfig[networkValue].vendors || [];
            console.log('Found vendors from config:', vendors);
            
            vendorSelect.innerHTML = '<option value="">Select Vendor</option>';
            
            if (vendors.length > 0) {
                vendors.forEach(vendor => {
                    const option = document.createElement('option');
                    option.value = vendor;
                    option.textContent = vendor;
                    vendorSelect.appendChild(option);
                });
            } else {
                vendorSelect.innerHTML = '<option value="">No vendors available for this network</option>';
            }
        } else {
            vendorSelect.innerHTML = '<option value="">No vendors configured for this network</option>';
        }
    }

    async updateQuarterDropdown(networkValue) {
        const quarterSelect = document.getElementById('updateQuarter');
        if (!quarterSelect) return;

        if (!networkValue) {
            quarterSelect.innerHTML = '<option value="">Select Network First</option>';
            return;
        }

        console.log('Updating quarters for network:', networkValue);
        
        // Use networkConfig data instead of API call
        if (this.networkConfig && this.networkConfig[networkValue]) {
            const quarters = this.networkConfig[networkValue].quarters || [];
            console.log('Found quarters from config:', quarters);
            
            quarterSelect.innerHTML = '<option value="">Select Quarter</option>';
            
            if (quarters.length > 0) {
                quarters.forEach(quarter => {
                    const option = document.createElement('option');
                    
                    // Handle both old string format and new object format
                    if (typeof quarter === 'string') {
                        // Old format - just a string
                        option.value = quarter;
                        option.textContent = quarter;
                    } else if (typeof quarter === 'object' && quarter.name) {
                        // New format - object with properties
                        option.value = quarter.name;
                        option.textContent = `${quarter.name} (${quarter.monthRange})`;
                        
                        // Only show active quarters in dropdown
                        if (quarter.active === false) {
                            return; // Skip inactive quarters
                        }
                    }
                    
                    quarterSelect.appendChild(option);
                });
            } else {
                quarterSelect.innerHTML = '<option value="">No quarters available for this network</option>';
            }
        } else {
            quarterSelect.innerHTML = '<option value="">No quarters configured for this network</option>';
        }
    }

    calculateBillWithoutTax(billWithTaxValue) {
        const billWithoutTaxInput = document.getElementById('updateBillWithoutTax');
        if (!billWithoutTaxInput) return;

        const billWithTax = parseFloat(billWithTaxValue);
        if (isNaN(billWithTax) || billWithTax <= 0) {
            billWithoutTaxInput.value = '';
            return;
        }

        // Calculate bill without tax: Bill With Tax / 1.18
        const billWithoutTax = billWithTax / 1.18;
        billWithoutTaxInput.value = billWithoutTax.toFixed(2);
    }

    calculateBillWithTax(billWithoutTaxValue) {
        const billWithTaxInput = document.getElementById('updateBillWithTax');
        if (!billWithTaxInput) return;

        const billWithoutTax = parseFloat(billWithoutTaxValue);
        if (isNaN(billWithoutTax) || billWithoutTax <= 0) {
            billWithTaxInput.value = '';
            return;
        }

        // Calculate bill with tax: Bill Without Tax * 1.18
        const billWithTax = billWithoutTax * 1.18;
        billWithTaxInput.value = billWithTax.toFixed(2);
    }

    async saveBill() {
        const saveButton = document.querySelector('#updateBillForm button[type="submit"]');
        const isEditing = saveButton.dataset.editingSerialNo;
        
        const formData = {
            network: document.getElementById('updateNetwork').value,
            vendor: document.getElementById('updateVendor').value,
            quarterString: document.getElementById('updateQuarter').value, // Use quarterString instead of quarter
            location: document.getElementById('updateLocation').value,
            invoiceNumber: document.getElementById('updateInvoiceNumber').value,
            billWithTax: parseFloat(document.getElementById('updateBillWithTax').value),
            billWithoutTax: parseFloat(document.getElementById('updateBillWithoutTax').value),
            ses1: parseInt(document.getElementById('updateSes1').value),
            ses2: parseInt(document.getElementById('updateSes2').value),
            billingPeriod: document.getElementById('updateBillingPeriod').value,
            fromDate: document.getElementById('updateFromDate').value,
            toDate: document.getElementById('updateToDate').value,
            status: document.getElementById('updateStatus').value || 'Pending',
            glCode: document.getElementById('updateGlCode').value,
            commitItem: document.getElementById('updateCommitItem').value,
            costCenter: document.getElementById('updateCostCenter').value,
            remarks: document.getElementById('updateRemarks').value
        };

        // If editing, preserve the existing PDF path
        if (isEditing) {
            const existingBill = this.bills.find(b => b.serialNo == isEditing);
            if (existingBill && existingBill.pdfFilePath) {
                formData.pdfFilePath = existingBill.pdfFilePath;
                console.log('DEBUG: Preserving existing PDF path:', existingBill.pdfFilePath);
            }
        }

        // Debug logging
        console.log('Form data:', formData);

        // Validation
        if (!formData.network || !formData.vendor || !formData.quarterString || !formData.location || 
            !formData.invoiceNumber || !formData.billWithTax || !formData.billWithoutTax ||
            !formData.ses1 || !formData.ses2 || !formData.fromDate || !formData.toDate ||
            !formData.glCode || !formData.commitItem || !formData.costCenter) {
            
            // More detailed validation feedback
            const missingFields = [];
            if (!formData.network) missingFields.push('Network');
            if (!formData.vendor) missingFields.push('Vendor');
            if (!formData.quarterString) missingFields.push('Quarter');
            if (!formData.location) missingFields.push('Location');
            if (!formData.invoiceNumber) missingFields.push('Invoice Number');
            if (!formData.billWithTax) missingFields.push('Bill With Tax');
            if (!formData.billWithoutTax) missingFields.push('Bill Without Tax');
            if (!formData.ses1) missingFields.push('SES1');
            if (!formData.ses2) missingFields.push('SES2');
            if (!formData.fromDate) missingFields.push('From Date');
            if (!formData.toDate) missingFields.push('To Date');
            if (!formData.glCode) missingFields.push('GL Code');
            if (!formData.commitItem) missingFields.push('Commit Item');
            if (!formData.costCenter) missingFields.push('Cost Center');
            
            this.showAlert(`Please fill in the following required fields: ${missingFields.join(', ')}`, 'warning');
            return;
        }

        try {
            let data;
            if (isEditing) {
                // Update existing bill
                data = await this.apiCall(`/api/bills/${isEditing}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(formData)
                });
            } else {
                // Create new bill
                data = await this.apiCall('/api/bills', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(formData)
                });
            }

            console.log('Save bill response data:', data);
            
            if (data && !data.error) {
                // Handle PDF upload if a file is selected
                const pdfFile = document.getElementById('updatePdfFile').files[0];
                console.log('PDF file selected:', pdfFile); // Debug log
                
                if (pdfFile) {
                    // For new bills, get serial number from response, for existing bills use the editing serial number
                    const serialNo = isEditing ? isEditing : (data.serialNo || this.bills.length + 1);
                    console.log('Uploading PDF for serialNo:', serialNo); // Debug log
                    await this.handlePdfUpload(serialNo, pdfFile);
                }
                
                this.showAlert(isEditing ? 'Bill updated successfully!' : 'Bill added successfully!', 'success');
                
                // Reset form and button
                document.getElementById('updateBillForm').reset();
                saveButton.innerHTML = '<i class="fas fa-save me-2"></i>Save Entry';
                delete saveButton.dataset.editingSerialNo;
                
                // Reload data
                await this.loadBills();
                this.populateFilters();
                this.applyFilters();
                
                // Refresh analytics if analytics tab is currently active
                const analyticsTab = document.getElementById('analytics-tab');
                if (analyticsTab && analyticsTab.classList.contains('active')) {
                    this.loadAnalytics();
                }
                
                // Show success and suggest switching to view tab
                setTimeout(() => {
                    if (confirm((isEditing ? 'Bill updated' : 'Bill added') + ' successfully! Would you like to view all bills?')) {
                        const viewTab = document.getElementById('view-tab');
                        const bootstrap_tab = new bootstrap.Tab(viewTab);
                        bootstrap_tab.show();
                    }
                }, 500);
            } else {
                this.showAlert('Failed to ' + (isEditing ? 'update' : 'add') + ' bill: ' + (data.message || 'Unknown error'), 'danger');
            }
        } catch (error) {
            this.showAlert('Failed to ' + (isEditing ? 'update' : 'add') + ' bill: ' + error.message, 'danger');
        }
    }

    async loadAnalytics() {
        // Populate filter dropdowns first
        this.populateAnalyticsFilters();
        
        // Apply filters and create summary table
        const filteredBills = this.getFilteredAnalyticsBills();
        const analyticsData = this.calculateAnalytics(filteredBills);
        this.renderAnalyticsTable(analyticsData);
        this.renderCharts(analyticsData);
    }

    populateAnalyticsFilters() {
        // Get unique values for filters
        const years = new Set();
        const quarters = new Set();
        const networks = new Set();
        const vendors = new Set();
        const glCodes = new Set();
        const costCenters = new Set();
        const commitItems = new Set();

        this.bills.forEach(bill => {
            // Extract year from fromDate or toDate
            const year = this.extractYearFromBill(bill);
            if (year) years.add(year);
            
            if (bill.quarterString || bill.quarter) quarters.add(bill.quarterString || bill.quarter);
            if (bill.network) networks.add(bill.network);
            if (bill.vendor) vendors.add(bill.vendor);
            if (bill.glCode) glCodes.add(bill.glCode);
            if (bill.costCenter) costCenters.add(bill.costCenter);
            if (bill.commitItem) commitItems.add(bill.commitItem);
        });

        // Populate dropdowns
        this.populateFilterDropdown('analyticsYearFilter', Array.from(years).sort());
        this.populateFilterDropdown('analyticsQuarterFilter', Array.from(quarters).sort());
        this.populateFilterDropdown('analyticsNetworkFilter', Array.from(networks).sort());
        this.populateFilterDropdown('analyticsVendorFilter', Array.from(vendors).sort());
        this.populateFilterDropdown('analyticsGlCodeFilter', Array.from(glCodes).sort());
        this.populateFilterDropdown('analyticsCostCenterFilter', Array.from(costCenters).sort());
        this.populateFilterDropdown('analyticsCommitItemFilter', Array.from(commitItems).sort());
        
        // Initialize vendor filter dependency
        this.updateAnalyticsVendorFilterForNetwork();
    }

    updateVendorFilterForNetwork() {
        const networkFilter = document.getElementById('networkFilter');
        const vendorFilter = document.getElementById('vendorFilter');
        
        if (!networkFilter || !vendorFilter) return;
        
        const selectedNetwork = networkFilter.value;
        
        // Clear and repopulate vendor filter based on selected network
        vendorFilter.innerHTML = '<option value="">All Vendors</option>';
        
        if (selectedNetwork && this.networkConfig && this.networkConfig[selectedNetwork]) {
            // Show only vendors for the selected network
            const vendors = this.networkConfig[selectedNetwork].vendors || [];
            vendors.forEach(vendor => {
                const option = document.createElement('option');
                option.value = vendor;
                option.textContent = vendor;
                vendorFilter.appendChild(option);
            });
        } else {
            // Show all vendors from bills and network config
            const vendorsFromBills = [...new Set(this.bills.map(b => b.vendor).filter(Boolean))];
            const vendorsFromConfig = [];
            if (this.networkConfig) {
                Object.values(this.networkConfig).forEach(config => {
                    if (config.vendors) {
                        vendorsFromConfig.push(...config.vendors);
                    }
                });
            }
            const allVendors = [...new Set([...vendorsFromBills, ...vendorsFromConfig])];
            
            allVendors.forEach(vendor => {
                const option = document.createElement('option');
                option.value = vendor;
                option.textContent = vendor;
                vendorFilter.appendChild(option);
            });
        }
    }

    updateAnalyticsVendorFilterForNetwork() {
        const networkFilter = document.getElementById('analyticsNetworkFilter');
        const vendorFilter = document.getElementById('analyticsVendorFilter');
        
        if (!networkFilter || !vendorFilter) return;
        
        const selectedNetwork = networkFilter.value;
        
        // Clear and repopulate vendor filter based on selected network
        vendorFilter.innerHTML = '<option value="">All Vendors</option>';
        
        if (selectedNetwork && this.networkConfig && this.networkConfig[selectedNetwork]) {
            // Show only vendors for the selected network
            const vendors = this.networkConfig[selectedNetwork].vendors || [];
            vendors.forEach(vendor => {
                const option = document.createElement('option');
                option.value = vendor;
                option.textContent = vendor;
                vendorFilter.appendChild(option);
            });
        } else {
            // Show all vendors from bills and network config
            const vendorsFromBills = [...new Set(this.bills.map(b => b.vendor).filter(Boolean))];
            const vendorsFromConfig = [];
            if (this.networkConfig) {
                Object.values(this.networkConfig).forEach(config => {
                    if (config.vendors) {
                        vendorsFromConfig.push(...config.vendors);
                    }
                });
            }
            const allVendors = [...new Set([...vendorsFromBills, ...vendorsFromConfig])];
            
            allVendors.forEach(vendor => {
                const option = document.createElement('option');
                option.value = vendor;
                option.textContent = vendor;
                vendorFilter.appendChild(option);
            });
        }
    }

    extractYearFromBill(bill) {
        // Try to extract year from fromDate, toDate, or billingPeriod
        if (bill.fromDate) {
            const date = new Date(bill.fromDate);
            if (!isNaN(date.getTime())) return date.getFullYear().toString();
        }
        if (bill.toDate) {
            const date = new Date(bill.toDate);
            if (!isNaN(date.getTime())) return date.getFullYear().toString();
        }
        if (bill.billingPeriod) {
            // Try to extract year from billing period string
            const yearMatch = bill.billingPeriod.match(/\d{4}/);
            if (yearMatch) return yearMatch[0];
        }
        return null;
    }

    populateFilterDropdown(selectId, options) {
        const select = document.getElementById(selectId);
        if (!select) return;
        
        // Keep the "All" option and current selection
        const currentValue = select.value;
        const allOption = select.querySelector('option[value=""]');
        select.innerHTML = '';
        if (allOption) select.appendChild(allOption);
        
        options.forEach(option => {
            const optionElement = document.createElement('option');
            optionElement.value = option;
            optionElement.textContent = option;
            select.appendChild(optionElement);
        });
        
        // Restore selection if it still exists
        if (currentValue && options.includes(currentValue)) {
            select.value = currentValue;
        }
    }

    getFilteredAnalyticsBills() {
        const yearFilter = document.getElementById('analyticsYearFilter')?.value || '';
        const quarterFilter = document.getElementById('analyticsQuarterFilter')?.value || '';
        const networkFilter = document.getElementById('analyticsNetworkFilter')?.value || '';
        const vendorFilter = document.getElementById('analyticsVendorFilter')?.value || '';
        const glCodeFilter = document.getElementById('analyticsGlCodeFilter')?.value || '';
        const costCenterFilter = document.getElementById('analyticsCostCenterFilter')?.value || '';
        const commitItemFilter = document.getElementById('analyticsCommitItemFilter')?.value || '';

        console.log('DEBUG: Applied analytics filters:', {
            year: yearFilter,
            quarter: quarterFilter,
            network: networkFilter,
            vendor: vendorFilter,
            glCode: glCodeFilter,
            costCenter: costCenterFilter,
            commitItem: commitItemFilter
        });

        const filteredBills = this.bills.filter(bill => {
            // Year filter
            if (yearFilter) {
                const billYear = this.extractYearFromBill(bill);
                if (billYear !== yearFilter) return false;
            }

            // Quarter filter
            if (quarterFilter) {
                const billQuarter = bill.quarterString || bill.quarter || '';
                if (billQuarter !== quarterFilter) return false;
            }

            // Network filter
            if (networkFilter && bill.network !== networkFilter) return false;

            // Vendor filter
            if (vendorFilter && bill.vendor !== vendorFilter) return false;

            // GL Code filter
            if (glCodeFilter && bill.glCode !== glCodeFilter) return false;

            // Cost Center filter
            if (costCenterFilter && bill.costCenter !== costCenterFilter) return false;

            // Commit Item filter
            if (commitItemFilter && bill.commitItem !== commitItemFilter) return false;

            return true;
        });

        console.log('DEBUG: Filtered bills count:', filteredBills.length, 'out of', this.bills.length, 'total bills');
        return filteredBills;
    }

    calculateAnalytics(bills = this.bills) {
        const summary = {};
        
        bills.forEach(bill => {
            const year = this.extractYearFromBill(bill) || 'Unknown';
            const key = `${year}-${bill.network || 'Unknown'}-${bill.vendor || 'Unknown'}`;
            if (!summary[key]) {
                summary[key] = {
                    year: year,
                    network: bill.network || 'Unknown',
                    vendor: bill.vendor || 'Unknown',
                    totalWithTax: 0,
                    totalWithoutTax: 0,
                    glCodes: new Set(),
                    commitItems: new Set(),
                    costCenters: new Set()
                };
            }
            
            summary[key].totalWithTax += (bill.billWithTax || 0);
            summary[key].totalWithoutTax += (bill.billWithoutTax || 0);
            
            if (bill.glCode) summary[key].glCodes.add(bill.glCode);
            if (bill.commitItem) summary[key].commitItems.add(bill.commitItem);
            if (bill.costCenter) summary[key].costCenters.add(bill.costCenter);
        });

        // Convert sets to arrays for display
        Object.values(summary).forEach(item => {
            item.glCodes = Array.from(item.glCodes);
            item.commitItems = Array.from(item.commitItems);
            item.costCenters = Array.from(item.costCenters);
        });

        return summary;
    }

    renderAnalyticsTable(analyticsData) {
        const tbody = document.getElementById('analyticsTableBody');
        tbody.innerHTML = '';

        Object.values(analyticsData).forEach(item => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${item.year}</td>
                <td>${item.network}</td>
                <td>${item.vendor}</td>
                <td>\u20b9${item.totalWithTax.toLocaleString('en-IN', {minimumFractionDigits: 2})}</td>
                <td>\u20b9${item.totalWithoutTax.toLocaleString('en-IN', {minimumFractionDigits: 2})}</td>
                <td>${item.glCodes.join(', ')}</td>
                <td>${item.commitItems.join(', ')}</td>
                <td>${item.costCenters.join(', ')}</td>
            `;
            tbody.appendChild(row);
        });
    }

    renderCharts(analyticsData) {
        // Destroy existing charts
        Object.values(this.charts).forEach(chart => {
            if (chart) chart.destroy();
        });

        // Get filtered bills for consistent chart generation
        const filteredBills = this.getFilteredAnalyticsBills();
        console.log('DEBUG: Rendering charts with filtered bills:', filteredBills.length, 'out of', this.bills.length, 'total bills');

        // Quarter Chart - based on filtered bills
        const quarterData = {};
        filteredBills.forEach(bill => {
            const quarter = bill.quarterString || bill.quarter || 'Unknown';
            quarterData[quarter] = (quarterData[quarter] || 0) + (bill.billWithTax || 0);
        });
        console.log('DEBUG: Quarter chart data:', quarterData);

        const quarterCtx = document.getElementById('quarterChart');
        if (quarterCtx && Object.keys(quarterData).length > 0) {
            this.charts.quarterChart = new Chart(quarterCtx, {
                type: 'line',
                data: {
                    labels: Object.keys(quarterData),
                    datasets: [{
                        label: 'Total Amount (\u20b9)',
                        data: Object.values(quarterData),
                        borderColor: 'rgba(75, 192, 192, 1)',
                        backgroundColor: 'rgba(75, 192, 192, 0.2)',
                        tension: 0.1
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        title: {
                            display: true,
                            text: 'Total Amount by Quarter (Filtered)'
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function(value) {
                                    return '\u20b9' + value.toLocaleString('en-IN');
                                }
                            }
                        }
                    }
                }
            });
        }

        // Year Chart - based on filtered bills
        const yearData = {};
        filteredBills.forEach(bill => {
            const year = this.extractYearFromBill(bill) || 'Unknown';
            yearData[year] = (yearData[year] || 0) + (bill.billWithTax || 0);
        });
        console.log('DEBUG: Year chart data:', yearData);

        const yearCtx = document.getElementById('yearChart');
        if (yearCtx && Object.keys(yearData).length > 0) {
            this.charts.yearChart = new Chart(yearCtx, {
                type: 'line',
                data: {
                    labels: Object.keys(yearData).sort(),
                    datasets: [{
                        label: 'Total Amount (\u20b9)',
                        data: Object.keys(yearData).sort().map(year => yearData[year]),
                        borderColor: 'rgba(255, 99, 132, 1)',
                        backgroundColor: 'rgba(255, 99, 132, 0.2)',
                        tension: 0.1
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        title: {
                            display: true,
                            text: 'Total Amount by Year (Filtered)'
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });
        }
    }

    loadNetworkManagement() {
        console.log('Loading network management...');
        console.log('Bills count:', this.bills.length);
        console.log('Network config:', this.networkConfig);
        console.log('Networks array:', this.networks);
        
        this.generateQuarterInputs(4); // Default to 4 quarters
        this.renderNetworksTable();
        this.setupVendorFieldHandlers();
        
        // Update the vendor management section header
        const vendorHeader = document.querySelector('#network-panel .card.border-info .card-header h6');
        if (vendorHeader) {
            vendorHeader.textContent = 'Select a network to view vendors';
        }
    }

    setupVendorFieldHandlers() {
        // Set up remove vendor field handlers
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('remove-vendor')) {
                const vendorInputs = document.getElementById('vendorInputs');
                if (vendorInputs.children.length > 1) {
                    e.target.closest('.vendor-input').remove();
                }
            }
        });
    }

    addVendorField() {
        const vendorInputs = document.getElementById('vendorInputs');
        const vendorInput = document.createElement('div');
        vendorInput.className = 'input-group mb-2 vendor-input';
        vendorInput.innerHTML = `
            <input type="text" class="form-control" placeholder="Enter vendor name">
            <button type="button" class="btn btn-outline-danger remove-vendor" title="Remove">\u00D7;</button>
        `;
        vendorInputs.appendChild(vendorInput);
    }

    generateQuarterInputs(count) {
        const quarterInputs = document.getElementById('quarterInputs');
        quarterInputs.innerHTML = '';

        for (let i = 1; i <= count; i++) {
            const quarterDiv = document.createElement('div');
            quarterDiv.className = 'mb-3';
            quarterDiv.innerHTML = `
                <div class="row">
                    <div class="col-12">
                        <label class="form-label">Q${i} Name:</label>
                        <input type="text" class="form-control quarter-name" placeholder="Q${i} (Apr-Jun)" data-quarter="${i}">
                    </div>
                </div>
                <div class="row">
                    <div class="col-6">
                        <label class="form-label">From:</label>
                        <select class="form-select quarter-from" data-quarter="${i}">
                            <option value="Jan">Jan</option>
                            <option value="Feb">Feb</option>
                            <option value="Mar">Mar</option>
                            <option value="Apr" ${i === 1 ? 'selected' : ''}>Apr</option>
                            <option value="May">May</option>
                            <option value="Jun">Jun</option>
                            <option value="Jul" ${i === 2 ? 'selected' : ''}>Jul</option>
                            <option value="Aug">Aug</option>
                            <option value="Sep">Sep</option>
                            <option value="Oct" ${i === 3 ? 'selected' : ''}>Oct</option>
                            <option value="Nov">Nov</option>
                            <option value="Dec">Dec</option>
                        </select>
                    </div>
                    <div class="col-6">
                        <label class="form-label">To:</label>
                        <select class="form-select quarter-to" data-quarter="${i}">
                            <option value="Jan" ${i === 4 ? 'selected' : ''}>Jan</option>
                            <option value="Feb">Feb</option>
                            <option value="Mar" ${i === 4 ? 'selected' : ''}>Mar</option>
                            <option value="Apr">Apr</option>
                            <option value="May">May</option>
                            <option value="Jun" ${i === 1 ? 'selected' : ''}>Jun</option>
                            <option value="Jul">Jul</option>
                            <option value="Aug">Aug</option>
                            <option value="Sep" ${i === 2 ? 'selected' : ''}>Sep</option>
                            <option value="Oct">Oct</option>
                            <option value="Nov">Nov</option>
                            <option value="Dec" ${i === 3 ? 'selected' : ''}>Dec</option>
                        </select>
                    </div>
                </div>
            `;
            quarterInputs.appendChild(quarterDiv);
        }
    }

    async addNetworkWithConfig() {
        const networkName = document.getElementById('networkName').value.trim();
        
        if (!networkName) {
            this.showAlert('Please enter a network name', 'warning');
            return;
        }

        // Check if network already exists
        if (this.networkConfig && this.networkConfig[networkName]) {
            this.showAlert('Network already exists', 'warning');
            return;
        }

        // Collect vendors
        const vendorInputs = document.querySelectorAll('#vendorInputs input');
        const vendors = [];
        vendorInputs.forEach(input => {
            const vendor = input.value.trim();
            if (vendor) vendors.push(vendor);
        });

        if (vendors.length === 0) {
            this.showAlert('Please add at least one vendor', 'warning');
            return;
        }

        // Collect quarters
        const quarterCount = parseInt(document.getElementById('quarterCount').value);
        const quarters = [];
        for (let i = 1; i <= quarterCount; i++) {
            const nameInput = document.querySelector(`.quarter-name[data-quarter="${i}"]`);
            const fromSelect = document.querySelector(`.quarter-from[data-quarter="${i}"]`);
            const toSelect = document.querySelector(`.quarter-to[data-quarter="${i}"]`);
            
            const quarterName = nameInput.value.trim() || `Q${i}`;
            quarters.push({
                name: quarterName,
                from: fromSelect.value,
                to: toSelect.value
            });
        }

        // Create network configuration
        const newNetworkConfig = {
            vendors: vendors,
            quarters: quarters
        };

        // Initialize networkConfig if it doesn't exist
        if (!this.networkConfig) {
            this.networkConfig = {};
        }

        this.networkConfig[networkName] = newNetworkConfig;

        // Save to backend
        await this.saveNetworkConfigToBackend();
        
        // Update local arrays for compatibility
        if (!this.networks.includes(networkName)) {
            this.networks.push(networkName);
        }
        vendors.forEach(vendor => {
            if (!this.vendors.includes(vendor)) {
                this.vendors.push(vendor);
            }
        });
        quarters.forEach(quarter => {
            if (!this.quarters.includes(quarter.name)) {
                this.quarters.push(quarter.name);
            }
        });

        // Clear form and refresh UI
        document.getElementById('addNetworkForm').reset();
        this.generateQuarterInputs(4);
        this.renderNetworksTable();
        this.populateUpdateForm(); // Update the Update tab dropdowns
        this.populateFilters(); // Update all filter dropdowns in View tab
        
        this.showAlert('Network added successfully', 'success');
    }

    renderNetworksTable() {
        const tbody = document.getElementById('networksTableBody');
        tbody.innerHTML = '';

        // Only use networks that have been manually added through the UI
        if (!this.networkConfig || Object.keys(this.networkConfig).length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted"><i class="fas fa-info-circle me-2"></i>No networks configured yet. Add your first network using the form on the left.</td></tr>';
            // Clear vendors table as well
            const vendorTbody = document.getElementById('vendorsTableBody');
            if (vendorTbody) {
                vendorTbody.innerHTML = '<tr><td colspan="2" class="text-center text-muted"><i class="fas fa-arrow-left me-2"></i>Select a network to view its vendors</td></tr>';
            }
            return;
        }

        Object.keys(this.networkConfig).forEach(networkName => {
            const config = this.networkConfig[networkName];
            const vendorCount = config.vendors ? config.vendors.length : 0;
            const quarterCount = config.quarters ? config.quarters.length : 0;

            const row = document.createElement('tr');
            row.style.cursor = 'pointer';
            row.innerHTML = `
                <td><strong>${networkName}</strong></td>
                <td><span class="badge bg-info">${vendorCount}</span></td>
                <td><span class="badge bg-secondary">${quarterCount}</span></td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary btn-sm" onclick="tracker.editNetwork('${networkName}')" title="Edit Network">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button class="btn btn-outline-warning btn-sm" onclick="tracker.showNetworkQuarters('${networkName}')" title="Manage Quarters">
                            <i class="fas fa-calendar"></i> Quarters
                        </button>
                        <button class="btn btn-outline-danger btn-sm" onclick="tracker.deleteNetwork('${networkName}')" title="Delete Network">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </td>
            `;
            
            // Add click handler to show vendors when row is clicked
            row.addEventListener('click', (e) => {
                // Don't trigger if button was clicked
                if (!e.target.closest('button')) {
                    this.showNetworkVendors(networkName);
                    // Highlight selected row
                    tbody.querySelectorAll('tr').forEach(tr => tr.classList.remove('table-active'));
                    row.classList.add('table-active');
                }
            });
            
            tbody.appendChild(row);
        });

        // Also update vendor display for the first network
        const firstNetwork = Object.keys(this.networkConfig)[0];
        if (firstNetwork) {
            this.showNetworkVendors(firstNetwork);
            // Highlight first row
            tbody.querySelector('tr')?.classList.add('table-active');
        }
    }

    showNetworkVendors(networkName) {
        const tbody = document.getElementById('vendorsTableBody');
        tbody.innerHTML = '';

        if (!this.networkConfig || !this.networkConfig[networkName]) {
            tbody.innerHTML = '<tr><td colspan="2" class="text-center text-muted">No vendors found for this network</td></tr>';
            return;
        }

        const vendors = this.networkConfig[networkName].vendors || [];
        
        if (vendors.length === 0) {
            tbody.innerHTML = '<tr><td colspan="2" class="text-center text-muted">No vendors configured for this network</td></tr>';
            return;
        }

        vendors.forEach((vendor, index) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td><strong>${vendor}</strong></td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary btn-sm" onclick="tracker.editNetworkVendor('${networkName}', ${index})" title="Edit Vendor">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-outline-danger btn-sm" onclick="tracker.deleteNetworkVendor('${networkName}', ${index})" title="Delete Vendor">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
        });

        // Update header to show selected network
        const header = document.querySelector('#network-panel .card.border-info .card-header h6');
        if (header) {
            header.textContent = `Vendors for ${networkName} (${vendors.length} vendor${vendors.length !== 1 ? 's' : ''})`;
        }
    }

    editNetwork(networkName) {
        if (!this.networkConfig || !this.networkConfig[networkName]) {
            this.showAlert('Network not found', 'error');
            return;
        }

        const config = this.networkConfig[networkName];
        const newNetworkName = prompt(`Edit network name:`, networkName);
        
        if (newNetworkName && newNetworkName.trim() && newNetworkName.trim() !== networkName) {
            const trimmedName = newNetworkName.trim();
            
            // Check if new name already exists
            if (this.networkConfig[trimmedName]) {
                this.showAlert('A network with this name already exists!', 'warning');
                return;
            }
            
            // Create new entry with new name
            this.networkConfig[trimmedName] = { ...config };
            
            // Remove old entry
            delete this.networkConfig[networkName];
            
            // Update networks array
            const networkIndex = this.networks.indexOf(networkName);
            if (networkIndex > -1) {
                this.networks[networkIndex] = trimmedName;
            }
            
            // Save and refresh
            this.saveNetworkConfigToBackend();
            this.renderNetworksTable();
            this.populateUpdateForm();
            this.populateFilters();
            
            this.showAlert('Network renamed successfully', 'success');
        }
    }

    showNetworkQuarters(networkName) {
        if (!this.networkConfig || !this.networkConfig[networkName]) {
            this.showAlert('Network not found', 'error');
            return;
        }

        const config = this.networkConfig[networkName];
        const quarters = config.quarters || [];
        
        // Create a modal dialog for quarter management
        const modalHtml = `
            <div class="modal fade" id="quarterModal" tabindex="-1">
                <div class="modal-dialog modal-xl">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Manage Quarters for ${networkName}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div class="mb-3">
                                <label class="form-label">Number of Quarters:</label>
                                <select class="form-select" id="editQuarterCount" style="width: 200px;">
                                    <option value="2" ${quarters.length === 2 ? 'selected' : ''}>2 Quarters</option>
                                    <option value="4" ${quarters.length === 4 ? 'selected' : ''}>4 Quarters</option>
                                </select>
                            </div>
                            <div class="alert alert-info">
                                <i class="fas fa-info-circle me-2"></i>
                                Configure each quarter with its name, number, and month range. This will be used for bill categorization and reporting.
                            </div>
                            <div id="editQuarterInputs">
                                <!-- Quarter inputs will be generated here -->
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="button" class="btn btn-primary" id="saveQuarters">
                                <i class="fas fa-save me-2"></i>Save Changes
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Remove existing modal if any
        const existingModal = document.getElementById('quarterModal');
        if (existingModal) {
            existingModal.remove();
        }

        // Add modal to body
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        
        // Generate quarter inputs
        this.generateEditQuarterInputs(quarters.length || 4, quarters);
        
        // Add event listener for quarter count change
        document.getElementById('editQuarterCount').addEventListener('change', (e) => {
            this.generateEditQuarterInputs(parseInt(e.target.value), quarters);
        });
        
        // Add save button event listener
        document.getElementById('saveQuarters').addEventListener('click', () => {
            this.saveNetworkQuarters(networkName);
        });
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('quarterModal'));
        modal.show();
        
        // Clean up modal when hidden
        document.getElementById('quarterModal').addEventListener('hidden.bs.modal', () => {
            document.getElementById('quarterModal').remove();
        });
    }

    async deleteNetwork(networkName) {
        if (!confirm(`Are you sure you want to delete network "${networkName}"? This will remove all associated vendors and quarters.`)) {
            return;
        }

        if (this.networkConfig && this.networkConfig[networkName]) {
            delete this.networkConfig[networkName];
            
            // Also remove from local arrays
            const networkIndex = this.networks.indexOf(networkName);
            if (networkIndex > -1) {
                this.networks.splice(networkIndex, 1);
            }

            await this.saveNetworkConfigToBackend();
            this.renderNetworksTable();
            this.populateUpdateForm(); // Update the Update tab dropdowns
            this.populateFilters(); // Update all filter dropdowns in View tab
            
            
            this.showAlert('Network deleted successfully', 'success');
        }
    }

    async editNetworkVendor(networkName, vendorIndex) {
        if (!this.networkConfig || !this.networkConfig[networkName]) {
            this.showAlert('Network not found', 'error');
            return;
        }

        const vendors = this.networkConfig[networkName].vendors;
        if (!vendors || vendorIndex >= vendors.length) {
            this.showAlert('Vendor not found', 'error');
            return;
        }

        const currentVendor = vendors[vendorIndex];
        const newVendor = prompt(`Edit vendor name:`, currentVendor);
        
        if (newVendor && newVendor.trim() && newVendor.trim() !== currentVendor) {
            vendors[vendorIndex] = newVendor.trim();
            await this.saveNetworkConfigToBackend();
            this.showNetworkVendors(networkName);
            this.populateUpdateForm(); // Update the Update tab dropdowns
            this.populateFilters(); // Update all filter dropdowns in View tab
            this.showAlert('Vendor updated successfully', 'success');
        }
    }

    async deleteNetworkVendor(networkName, vendorIndex) {
        if (!this.networkConfig || !this.networkConfig[networkName]) {
            this.showAlert('Network not found', 'error');
            return;
        }

        const vendors = this.networkConfig[networkName].vendors;
        if (!vendors || vendorIndex >= vendors.length) {
            this.showAlert('Vendor not found', 'error');
            return;
        }

        const vendorName = vendors[vendorIndex];
        if (!confirm(`Are you sure you want to delete vendor "${vendorName}"?`)) {
            return;
        }

        vendors.splice(vendorIndex, 1);
        
        await this.saveNetworkConfigToBackend();
        this.showNetworkVendors(networkName);
        this.renderNetworksTable(); // Update vendor count
        this.populateUpdateForm(); // Update the Update tab dropdowns
        this.populateFilters(); // Update all filter dropdowns in View tab
        
        this.showAlert('Vendor deleted successfully', 'success');
    }

    async saveNetworkConfigToBackend() {
        try {
            const data = await this.apiCall('/api/bills/config', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    networks: this.networks,
                    vendors: this.vendors,
                    quarters: this.quarters,
                    networkConfig: this.networkConfig
                })
            });

            if (data && data.error) {
                console.error('Failed to save network configuration:', data.error);
            }
        } catch (error) {
            console.error('Error saving network configuration:', error);
        }
    }

    async saveNetworkConfig() {
        // Compatibility method - calls the new save method
        await this.saveNetworkConfigToBackend();
    }

    async editBill(serialNo) {
        console.log('DEBUG: editBill called with serialNo:', serialNo);
        
        // Ensure bills is an array
        if (!Array.isArray(this.bills)) {
            console.log('Bills is not an array, cannot edit bill');
            this.showAlert('No bills data available', 'danger');
            return;
        }
        
        // Find the bill to edit
        const bill = this.bills.find(b => b.serialNo === serialNo);
        if (!bill) {
            console.log('DEBUG: Bill not found for serialNo:', serialNo);
            console.log('DEBUG: Available bills:', this.bills.map(b => b.serialNo));
            this.showAlert('Bill not found!', 'danger');
            return;
        }

        console.log('DEBUG: Found bill to edit:', bill);
        console.log('DEBUG: Bill properties:', Object.keys(bill));
        console.log('DEBUG: Bill values:', Object.entries(bill));

        // Switch to Update tab
        const updateTab = document.getElementById('update-tab');
        const bootstrap_tab = new bootstrap.Tab(updateTab);
        bootstrap_tab.show();

        // Wait for tab to be fully shown
        await new Promise(resolve => setTimeout(resolve, 200));

        // Store the bill data temporarily to prevent it from being overwritten
        window.editingBillData = bill;

        // First, populate the dropdowns with current data but don't trigger change events yet
        this.populateUpdateForm();
        
        // Wait for dropdowns to be populated
        await new Promise(resolve => setTimeout(resolve, 200));

        // Update vendors and quarters dropdowns based on selected network
        if (bill.network) {
            this.updateVendorDropdown(bill.network);
            this.updateQuarterDropdown(bill.network);
            
            // Wait for dropdowns to update
            await new Promise(resolve => setTimeout(resolve, 200));
        }

        // Now populate form with bill data - preserve all existing values
        console.log('DEBUG: Setting form values...');
        
        // Set dropdown values with explicit logging
        const networkSelect = document.getElementById('updateNetwork');
        if (networkSelect) {
            networkSelect.value = bill.network ?? '';
            console.log('DEBUG: Set network to:', bill.network, 'Selected value:', networkSelect.value);
        }
        
        // Wait a bit more to ensure network change is processed
        await new Promise(resolve => setTimeout(resolve, 100));
        
        const vendorSelect = document.getElementById('updateVendor');
        if (vendorSelect) {
            vendorSelect.value = bill.vendor ?? '';
            console.log('DEBUG: Set vendor to:', bill.vendor, 'Selected value:', vendorSelect.value);
        }
        
        const quarterSelect = document.getElementById('updateQuarter');
        if (quarterSelect) {
            const quarterValue = bill.quarterString ?? bill.quarter ?? '';
            quarterSelect.value = quarterValue;
            console.log('DEBUG: Set quarter to:', quarterValue, 'Selected value:', quarterSelect.value);
        }
        
        const locationSelect = document.getElementById('updateLocation');
        if (locationSelect) {
            locationSelect.value = bill.location ?? '';
            console.log('DEBUG: Set location to:', bill.location, 'Selected value:', locationSelect.value);
        }

        // Set all text input values
        const fieldsToSet = [
            { id: 'updateInvoiceNumber', value: bill.invoiceNumber, label: 'Invoice Number' },
            { id: 'updateBillWithTax', value: bill.billWithTax, label: 'Bill With Tax' },
            { id: 'updateBillWithoutTax', value: bill.billWithoutTax, label: 'Bill Without Tax' },
            { id: 'updateSes1', value: bill.ses1, label: 'SES1' },
            { id: 'updateSes2', value: bill.ses2, label: 'SES2' },
            { id: 'updateBillingPeriod', value: bill.billingPeriod, label: 'Billing Period' },
            { id: 'updateFromDate', value: bill.fromDate, label: 'From Date' },
            { id: 'updateToDate', value: bill.toDate, label: 'To Date' },
            { id: 'updateStatus', value: bill.status ?? 'Pending', label: 'Status' },
            { id: 'updateGlCode', value: bill.glCode, label: 'GL Code' },
            { id: 'updateCommitItem', value: bill.commitItem, label: 'Commit Item' },
            { id: 'updateCostCenter', value: bill.costCenter, label: 'Cost Center' },
            { id: 'updateRemarks', value: bill.remarks, label: 'Remarks' }
        ];

        fieldsToSet.forEach(field => {
            const element = document.getElementById(field.id);
            if (element) {
                const value = field.value ?? '';
                element.value = value;
                console.log(`DEBUG: Set ${field.label} to:`, value);
            }
        });

        // Show PDF info if exists
        const currentPdfInfo = document.getElementById('currentPdfInfo');
        const currentPdfName = document.getElementById('currentPdfName');
        const downloadCurrentPdf = document.getElementById('downloadCurrentPdf');
        
        if (bill.pdfFilePath) {
            console.log('DEBUG: Bill has PDF:', bill.pdfFilePath);
            const fileName = bill.pdfFilePath.split(/[/\\]/).pop();
            currentPdfName.textContent = fileName;
            currentPdfInfo.style.display = 'block';
            downloadCurrentPdf.style.display = 'inline-block';
            downloadCurrentPdf.onclick = () => this.downloadPdf(bill.pdfFilePath);
        } else {
            currentPdfInfo.style.display = 'none';
            console.log('DEBUG: No PDF found for this bill');
        }

        console.log('DEBUG: All form values set');

        // Update the save button to show it's in edit mode
        const saveButton = document.querySelector('#updateBillForm button[type="submit"]');
        if (saveButton) {
            saveButton.innerHTML = '<i class="fas fa-edit me-2"></i>Update Entry';
            saveButton.dataset.editingSerialNo = serialNo;
        }

        // Clear the temporary storage
        delete window.editingBillData;

        this.showAlert('Editing bill #' + serialNo + '. All existing data has been loaded. Make your changes and click "Update Entry".', 'info');
    }

    async deleteBill(serialNo) {
        if (!confirm('Are you sure you want to delete this bill?')) {
            return;
        }

        try {
            const data = await this.apiCall(`/api/bills/${serialNo}`, {
                method: 'DELETE'
            });
            
            if (data && !data.error) {
                this.showAlert('Bill deleted successfully!', 'success');
                await this.loadBills();
                this.populateFilters();
                this.applyFilters();
            } else {
                this.showAlert('Failed to delete bill: ' + (data.message || 'Unknown error'), 'danger');
            }
        } catch (error) {
            this.showAlert('Failed to delete bill: ' + error.message, 'danger');
        }
    }

    viewPdf(pdfPath) {
        if (pdfPath) {
            console.log('DEBUG: Viewing PDF with path:', pdfPath);
            console.log('DEBUG: Type of pdfPath:', typeof pdfPath);
            console.log('DEBUG: Raw pdfPath:', JSON.stringify(pdfPath));
            
            // Clean the path - remove any extra quotes or escaping
            const cleanPath = typeof pdfPath === 'string' ? pdfPath : String(pdfPath);
            console.log('DEBUG: Clean path:', cleanPath);
            
            // Open PDF in new tab - send the full path to backend
            const encodedPath = encodeURIComponent(cleanPath);
            console.log('DEBUG: Encoded path:', encodedPath);
            
            const url = `/api/bills/pdf/${encodedPath}`;
            console.log('DEBUG: Final URL:', url);
            
            const link = document.createElement('a');
            link.href = url;
            link.target = '_blank';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
    }

    downloadPdf(pdfPath) {
        if (pdfPath) {
            console.log('DEBUG: Downloading PDF with path:', pdfPath);
            
            // Clean the path - remove any extra quotes or escaping
            let cleanPath = pdfPath.trim();
            console.log('DEBUG: Clean path:', cleanPath);
            
            // Extract filename for download attribute
            const filename = cleanPath.split(/[/\\]/).pop();
            console.log('DEBUG: Extracted filename for download:', filename);
            
            // Create the download URL with proper base URL
            const downloadUrl = `${this.baseUrl}/api/bills/pdf/${encodeURIComponent(cleanPath)}`;
            console.log('DEBUG: Download URL:', downloadUrl);
            
            // Use fetch to download with proper credentials and error handling
            fetch(downloadUrl, {
                method: 'GET',
                credentials: 'include'
            })
            .then(response => {
                console.log('DEBUG: Download response status:', response.status);
                
                if (!response.ok) {
                    throw new Error(`Download failed: ${response.status} ${response.statusText}`);
                }
                
                return response.blob();
            })
            .then(blob => {
                console.log('DEBUG: Downloaded blob size:', blob.size);
                
                // Create download link
                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.download = filename;
                link.style.display = 'none';
                
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                
                // Clean up the blob URL
                window.URL.revokeObjectURL(url);
                
                console.log('DEBUG: PDF download completed successfully');
            })
            .catch(error => {
                console.error('DEBUG: Download error:', error);
                this.showAlert(`Failed to download PDF: ${error.message}`, 'danger');
            });
        } else {
            console.log('DEBUG: No PDF path provided');
            this.showAlert('No PDF file available for download', 'warning');
        }
    }



    uploadPdf(serialNo) {
        console.log('🔄 uploadPdf called for serial:', serialNo);
        
        // Verify serialNo is valid
        if (!serialNo) {
            console.error('❌ Invalid serialNo:', serialNo);
            this.showAlert('Cannot upload PDF: Invalid bill reference', 'danger');
            return;
        }
        
        console.log('✅ Creating file input for bill #' + serialNo);
        
        // Create file input with a unique ID to prevent conflicts
        const inputId = `pdf-upload-${Date.now()}`;
        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.id = inputId;
        fileInput.name = 'file';  // Important: name must match what the server expects
        fileInput.accept = 'application/pdf,.pdf';
        fileInput.style.display = 'none';
        
        // Create a cleanup function we can call from different handlers
        const cleanupInput = () => {
            console.log('🧹 Cleaning up file input:', inputId);
            if (document.body.contains(fileInput)) {
                document.body.removeChild(fileInput);
            }
            // Remove focus listener if it exists
            window.removeEventListener('focus', onFocus);
        };
        
        // Create a separate form for the file upload - this helps prevent any issues
        const form = document.createElement('form');
        form.enctype = 'multipart/form-data';
        form.style.display = 'none';
        form.appendChild(fileInput);
        document.body.appendChild(form);
        
        // Handle file selection
        fileInput.onchange = async (e) => {
            try {
                // Check if files were selected
                if (!e.target.files || e.target.files.length === 0) {
                    console.error('❌ No files selected');
                    this.showAlert('No file selected', 'warning');
                    cleanupInput();
                    return;
                }
                
                const file = e.target.files[0];
                console.log('📄 File selected:', file.name, 'Size:', file.size, 'Type:', file.type);
                
                // Validate file type more thoroughly
                if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
                    this.showAlert('Please select a PDF file only', 'warning');
                    cleanupInput();
                    return;
                }
                
                // Validate file size (max 10MB)
                const maxSize = 10 * 1024 * 1024; // 10MB
                if (file.size > maxSize) {
                    this.showAlert('File size too large. Please select a file smaller than 10MB', 'warning');
                    cleanupInput();
                    return;
                }
                
                // Check if file is not empty
                if (file.size === 0) {
                    this.showAlert('Selected file is empty', 'warning');
                    cleanupInput();
                    return;
                }
                
                // Upload the file
                await this.handlePdfUpload(serialNo, file);
            } catch (error) {
                console.error('❌ Error handling file selection:', error);
                this.showAlert('Error selecting file: ' + error.message, 'danger');
            } finally {
                // Always clean up
                cleanupInput();
                if (document.body.contains(form)) {
                    document.body.removeChild(form);
                }
            }
        };
        
        // Handle cancellation using window focus
        const onFocus = () => {
            setTimeout(() => {
                // Check if input still exists and no file was selected
                const input = document.getElementById(inputId);
                if (input && (!input.files || input.files.length === 0)) {
                    console.log('📄 File selection likely cancelled (window focus detected)');
                    cleanupInput();
                    if (document.body.contains(form)) {
                        document.body.removeChild(form);
                    }
                }
            }, 1000);
        };
        
        // Add focus listener
        window.addEventListener('focus', onFocus);
        
        // Add a timeout to clean up even if the focus event never fires
        setTimeout(() => {
            const input = document.getElementById(inputId);
            if (input && (!input.files || input.files.length === 0)) {
                console.log('📄 File selection timed out or was cancelled');
                cleanupInput();
                if (document.body.contains(form)) {
                    document.body.removeChild(form);
                }
            }
        }, 60000); // 1 minute timeout as a safety net
        
        // Add to DOM and trigger click
        fileInput.click();
    }

    async handlePdfUpload(serialNo, file) {
        console.log('🔄 handlePdfUpload called with:', serialNo, file ? file.name : 'no file', 'Size:', file ? file.size : 'n/a', 'Type:', file ? file.type : 'n/a');
        
        // Validate file - more robust checking
        if (!file || file.size === 0) {
            console.error('❌ No file or empty file provided to handlePdfUpload');
            this.showAlert('No file selected', 'warning');
            return;
        }
        
        if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
            console.error('❌ File is not a PDF:', file.type);
            this.showAlert('Please select a PDF file', 'warning');
            return;
        }
        
        // Show loading indicator before authentication check
        this.showAlert('Preparing PDF upload...', 'info');
        
        // Check authentication first - use our apiCall method for consistency
        try {
            console.log('🔐 Verifying authentication before upload');
            const user = await this.apiCall('/api/auth/me');
            console.log('✅ Authentication verified for user:', user.username);
        } catch (error) {
            console.error('❌ Authentication check failed:', error);
            this.showAlert('Session expired. Please login again before uploading.', 'warning');
            this.currentUser = null;
            this.showLoginSection();
            return;
        }
        
        // Show upload progress indicator
        this.showAlert('Uploading PDF...', 'info');
        
        // Create form data - ensure file is properly appended
        const formData = new FormData();
        formData.append('file', file, file.name);
        
        // Log what's in the FormData for debugging
        console.log('📦 FormData contents:');
        for (let [key, value] of formData.entries()) {
            console.log(`  ${key}:`, value instanceof File ? `File: ${value.name}, size: ${value.size}, type: ${value.type}` : value);
        }

        try {
            // Create a direct form submission instead of using fetch API
            const fullUrl = `${this.baseUrl}/api/bills/${serialNo}/upload-pdf`;
            console.log('🔄 Full upload URL:', fullUrl);
            
            // Use proper fetch with explicit timeout
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 60000); // 1 minute timeout
            
            console.log('📤 Sending PDF upload request...');
            const response = await fetch(fullUrl, {
                method: 'POST',
                body: formData,
                credentials: 'include',
                signal: controller.signal,
                // Do not set Content-Type - browser will set it with proper boundary
            });
            clearTimeout(timeoutId);

            console.log('📡 Upload response status:', response.status, response.statusText);
            
            if (!response.ok) {
                // Try to get error details from response
                let errorMessage = `Upload failed: ${response.status} ${response.statusText}`;
                try {
                    const contentType = response.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        const errorData = await response.json();
                        if (errorData.error) {
                            errorMessage = errorData.error;
                        }
                    } else {
                        const errorText = await response.text();
                        if (errorText) {
                            errorMessage = errorText;
                        }
                    }
                } catch (e) {
                    console.error('Error parsing error response:', e);
                }
                throw new Error(errorMessage);
            }
            
            let data;
            const contentType = response.headers.get('content-type');
            console.log('📡 Response content-type:', contentType);
            
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                const text = await response.text();
                console.log('📡 Response text:', text);
                data = { success: true, message: text };
            }

            console.log('✅ PDF Upload response:', data);
            
            if (data && (data.success || !data.error)) {
                this.showAlert('PDF uploaded successfully!', 'success');
                await this.loadBills(); // Reload bills to get updated PDF path
                this.applyFilters(); // Refresh the view
            } else {
                this.showAlert('Failed to upload PDF: ' + (data.error || data.message || 'Unknown error'), 'danger');
            }
        } catch (error) {
            console.error('❌ PDF Upload error:', error);
            
            if (error.name === 'AbortError') {
                this.showAlert('Upload timed out. Please try again with a smaller file or check your internet connection.', 'danger');
            } else if (error.message.includes('401') || error.message.includes('Not authenticated')) {
                this.showAlert('Session expired. Please log in again.', 'warning');
                this.currentUser = null;
                this.showLoginSection();
            } else {
                this.showAlert('Failed to upload PDF: ' + error.message, 'danger');
            }
        }
    }

    exportData() {
        try {
            // Create workbook
            const wb = XLSX.utils.book_new();
            
            // Prepare data for Excel
            const headers = [
                'Serial No', 'Network', 'Vendor', 'Quarter', 'Location', 'Invoice Number',
                'Bill (With Tax)', 'Bill (Without Tax)', 'SES 1', 'SES 2', 'Billing Period',
                'From Date', 'To Date', 'GL Code', 'Commit Item', 'Cost Center', 'Status', 'Remarks'
            ];
            
            // Create data array with headers
            const data = [headers];
            
            // Add bill data
            this.filteredBills.forEach(bill => {
                data.push([
                    bill.serialNo || '',
                    bill.network || '',
                    bill.vendor || '',
                    bill.quarterString || bill.quarter || '',
                    bill.location || '',
                    bill.invoiceNumber || '',
                    bill.billWithTax || 0,
                    bill.billWithoutTax || 0,
                    bill.ses1 || '',
                    bill.ses2 || '',
                    bill.billingPeriod || '',
                    bill.fromDate || '',
                    bill.toDate || '',
                    bill.glCode || '',
                    bill.commitItem || '',
                    bill.costCenter || '',
                    bill.status || 'Pending',
                    bill.remarks || ''
                ]);
            });
            
            // Create worksheet
            const ws = XLSX.utils.aoa_to_sheet(data);
            
            // Set column widths
            const colWidths = [
                { wch: 10 }, // Serial No
                { wch: 15 }, // Network
                { wch: 20 }, // Vendor
                { wch: 12 }, // Quarter
                { wch: 25 }, // Location
                { wch: 20 }, // Invoice Number
                { wch: 15 }, // Bill (With Tax)
                { wch: 15 }, // Bill (Without Tax)
                { wch: 10 }, // SES 1
                { wch: 10 }, // SES 2
                { wch: 15 }, // Billing Period
                { wch: 12 }, // From Date
                { wch: 12 }, // To Date
                { wch: 12 }, // GL Code
                { wch: 15 }, // Commit Item
                { wch: 15 }, // Cost Center
                { wch: 12 }, // Status
                { wch: 30 }  // Remarks
            ];
            ws['!cols'] = colWidths;
            
            // Style the header row
            const headerRange = XLSX.utils.decode_range(ws['!ref']);
            for (let col = headerRange.s.c; col <= headerRange.e.c; col++) {
                const cellAddress = XLSX.utils.encode_cell({ r: 0, c: col });
                if (!ws[cellAddress]) continue;
                
                ws[cellAddress].s = {
                    font: { bold: true, color: { rgb: "FFFFFF" } },
                    fill: { fgColor: { rgb: "366092" } },
                    alignment: { horizontal: "center", vertical: "center" }
                };
            }
            
            // Add worksheet to workbook
            XLSX.utils.book_append_sheet(wb, ws, "Bills Data");
            
            // Create analytics summary sheet if there are bills
            if (this.filteredBills.length > 0) {
                const analyticsData = this.createAnalyticsForExport();
                const analyticsWs = XLSX.utils.aoa_to_sheet(analyticsData);
                
                // Set column widths for analytics
                analyticsWs['!cols'] = [
                    { wch: 8 },  // Year
                    { wch: 15 }, // Network
                    { wch: 20 }, // Vendor
                    { wch: 18 }, // Total With Tax
                    { wch: 18 }, // Total Without Tax
                    { wch: 15 }, // GL Codes
                    { wch: 15 }, // Commit Items
                    { wch: 15 }  // Cost Centers
                ];
                
                // Style the analytics header
                if (analyticsWs['!ref']) {
                    const analyticsHeaderRange = XLSX.utils.decode_range(analyticsWs['!ref']);
                    for (let col = analyticsHeaderRange.s.c; col <= analyticsHeaderRange.e.c; col++) {
                        const cellAddress = XLSX.utils.encode_cell({ r: 0, c: col });
                        if (!analyticsWs[cellAddress]) continue;
                        
                        analyticsWs[cellAddress].s = {
                            font: { bold: true, color: { rgb: "FFFFFF" } },
                            fill: { fgColor: { rgb: "28a745" } },
                            alignment: { horizontal: "center", vertical: "center" }
                        };
                    }
                }
                
                XLSX.utils.book_append_sheet(wb, analyticsWs, "Analytics Summary");
            }
            
            // Generate filename with timestamp
            const timestamp = new Date().toISOString().split('T')[0];
            const filename = `BSNL_Bills_Export_${timestamp}.xlsx`;
            
            // Save the file
            XLSX.writeFile(wb, filename);
            
            this.showAlert(`Excel file "${filename}" has been downloaded successfully!`, 'success');
            
        } catch (error) {
            console.error('Export error:', error);
            this.showAlert('Error exporting data to Excel. Please try again.', 'danger');
        }
    }

    createAnalyticsForExport() {
        const analyticsHeaders = [
            'Year', 'Network', 'Vendor', 'Total With Tax', 'Total Without Tax', 
            'GL Codes', 'Commit Items', 'Cost Centers'
        ];
        
        const analyticsData = [analyticsHeaders];
        
        // Calculate analytics
        const summary = {};
        this.filteredBills.forEach(bill => {
            const year = this.extractYearFromBill(bill) || 'Unknown';
            const key = `${year}-${bill.network || 'Unknown'}-${bill.vendor || 'Unknown'}`;
            
            if (!summary[key]) {
                summary[key] = {
                    year: year,
                    network: bill.network || 'Unknown',
                    vendor: bill.vendor || 'Unknown',
                    totalWithTax: 0,
                    totalWithoutTax: 0,
                    glCodes: new Set(),
                    commitItems: new Set(),
                    costCenters: new Set()
                };
            }
            
            summary[key].totalWithTax += (bill.billWithTax || 0);
            summary[key].totalWithoutTax += (bill.billWithoutTax || 0);
            
            if (bill.glCode) summary[key].glCodes.add(bill.glCode);
            if (bill.commitItem) summary[key].commitItems.add(bill.commitItem);
            if (bill.costCenter) summary[key].costCenters.add(bill.costCenter);
        });
        
        // Convert summary to rows
        Object.values(summary).forEach(item => {
            analyticsData.push([
                item.year,
                item.network,
                item.vendor,
                item.totalWithTax.toFixed(2),
                item.totalWithoutTax.toFixed(2),
                Array.from(item.glCodes).join(', '),
                Array.from(item.commitItems).join(', '),
                Array.from(item.costCenters).join(', ')
            ]);
        });
        
        return analyticsData;
    }

    showAlert(message, type) {
        // Create alert element
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        alertDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; max-width: 400px;';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        // Add to body
        document.body.appendChild(alertDiv);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.parentNode.removeChild(alertDiv);
            }
        }, 5000);
    }

    buildNetworkConfigFromBills() {
        console.log('Building network config from bills...');
        console.log('Bills array:', this.bills);
        console.log('Bills length:', this.bills ? this.bills.length : 'undefined');
        
        if (!this.bills || this.bills.length === 0) {
            console.log('No bills data available');
            return;
        }

        // Initialize networkConfig if it doesn't exist
        if (!this.networkConfig) {
            this.networkConfig = {};
        }

        // Group bills by network and collect vendors and quarters
        const networkData = {};
        
        this.bills.forEach(bill => {
            console.log('Processing bill:', bill);
            if (!bill.network) return;

            if (!networkData[bill.network]) {
                networkData[bill.network] = {
                    vendors: new Set(),
                    quarters: new Set()
                };
            }

            if (bill.vendor) {
                networkData[bill.network].vendors.add(bill.vendor);
            }

            // Use quarterString if available, otherwise fall back to quarter
            const quarterValue = bill.quarterString || bill.quarter;
            if (quarterValue) {
                networkData[bill.network].quarters.add(quarterValue);
            }
        });

        console.log('Network data collected:', networkData);

        // Convert to the expected format
        Object.keys(networkData).forEach(networkName => {
            if (!this.networkConfig[networkName]) {
                const vendors = Array.from(networkData[networkName].vendors);
                const quarterNames = Array.from(networkData[networkName].quarters);
                
                // Create quarters configuration (simplified)
                const quarters = quarterNames.map((name, index) => ({
                    name: name,
                    from: this.getQuarterStartMonth(name, index),
                    to: this.getQuarterEndMonth(name, index)
                }));

                this.networkConfig[networkName] = {
                    vendors: vendors,
                    quarters: quarters
                };
            }
        });
        
        console.log('Final network config:', this.networkConfig);
    }

    getQuarterStartMonth(quarterName, index) {
        // Try to extract month information from quarter name
        const monthMap = {
            'jan': 'Jan', 'feb': 'Feb', 'mar': 'Mar', 'apr': 'Apr',
            'may': 'May', 'jun': 'Jun', 'jul': 'Jul', 'aug': 'Aug',
            'sep': 'Sep', 'oct': 'Oct', 'nov': 'Nov', 'dec': 'Dec'
        };
        
        const lowerName = quarterName.toLowerCase();
        for (const [key, value] of Object.entries(monthMap)) {
            if (lowerName.includes(key)) {
                return value;
            }
        }
        
        // Default quarterly pattern
        const defaultStarts = ['Apr', 'Jul', 'Oct', 'Jan'];
        return defaultStarts[index % 4];
    }

    getQuarterEndMonth(quarterName, index) {
        // Try to extract end month from quarter name
        const monthMap = {
            'jan': 'Jan', 'feb': 'Feb', 'mar': 'Mar', 'apr': 'Apr',
            'may': 'May', 'jun': 'Jun', 'jul': 'Jul', 'aug': 'Aug',
            'sep': 'Sep', 'oct': 'Oct', 'nov': 'Nov', 'dec': 'Dec'
        };
        
        const lowerName = quarterName.toLowerCase();
        // Look for patterns like "Apr-Jun" or "Apr to Jun"
        const months = Object.keys(monthMap);
        let foundMonths = [];
        
        months.forEach(month => {
            if (lowerName.includes(month)) {
                foundMonths.push(monthMap[month]);
            }
        });
        
        if (foundMonths.length >= 2) {
            return foundMonths[foundMonths.length - 1]; // Return the last found month
        }
        
        // Default quarterly pattern
        const defaultEnds = ['Jun', 'Sep', 'Dec', 'Mar'];
        return defaultEnds[index % 4];
    }

    // Debug function to test functionality
    testEditAndDownload() {
        console.log('=== DEBUGGING EDIT AND DOWNLOAD FUNCTIONALITY ===');
        console.log('Total bills loaded:', this.bills.length);
        console.log('Bills array:', this.bills);
        
        if (this.bills.length > 0) {
            const firstBill = this.bills[0];
            console.log('First bill:', firstBill);
            console.log('First bill PDF path:', firstBill.pdfFilePath);
            console.log('First bill serial number:', firstBill.serialNo);
        }
        
        // Test if the functions exist
        console.log('editBill function exists:', typeof this.editBill === 'function');
        console.log('downloadPdf function exists:', typeof this.downloadPdf === 'function');
    }

    generateEditQuarterInputs(quarterCount, existingQuarters = []) {
        const container = document.getElementById('editQuarterInputs');
        if (!container) return;
        
        container.innerHTML = '';
        
        // Default month ranges for different quarters
        const defaultMonthRanges = {
            2: ['Apr-Sep', 'Oct-Mar'],
            4: ['Apr-Jun', 'Jul-Sep', 'Oct-Dec', 'Jan-Mar']
        };
        
        const monthRanges = defaultMonthRanges[quarterCount] || defaultMonthRanges[4];
        
        for (let i = 0; i < quarterCount; i++) {
            // Parse existing quarter data if available
            let existingData = {
                name: `Q${i + 1}`,
                number: i + 1,
                monthRange: monthRanges[i] || ''
            };
            
            if (existingQuarters[i]) {
                if (typeof existingQuarters[i] === 'string') {
                    // Old format - just a string
                    existingData.name = existingQuarters[i];
                } else if (typeof existingQuarters[i] === 'object') {
                    // New format - object with properties
                    existingData = {
                        name: existingQuarters[i].name || `Q${i + 1}`,
                        number: existingQuarters[i].number || (i + 1),
                        monthRange: existingQuarters[i].monthRange || monthRanges[i] || ''
                    };
                }
            }
            
            const quarterCard = document.createElement('div');
            quarterCard.className = 'card mb-3';
            quarterCard.innerHTML = `
                <div class="card-header">
                    <h6 class="mb-0">Quarter ${i + 1} Configuration</h6>
                </div>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-4">
                            <label class="form-label">Quarter Name:</label>
                            <input type="text" class="form-control quarter-name-input" 
                                   placeholder="e.g., Q1, First Quarter" 
                                   value="${existingData.name}">
                            <small class="form-text text-muted">Display name for this quarter</small>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Quarter Number:</label>
                            <input type="number" class="form-control quarter-number-input" 
                                   min="1" max="12" 
                                   placeholder="e.g., 1, 2, 3, 4" 
                                   value="${existingData.number}">
                            <small class="form-text text-muted">Numeric identifier for sorting</small>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Month Range:</label>
                            <input type="text" class="form-control quarter-months-input" 
                                   placeholder="e.g., Apr-Jun, Jul-Sep" 
                                   value="${existingData.monthRange}">
                            <small class="form-text text-muted">Month range for this quarter</small>
                        </div>
                    </div>
                    <div class="row mt-3">
                        <div class="col-12">
                            <div class="form-check">
                                <input class="form-check-input quarter-active-input" type="checkbox" 
                                       id="active${i}" ${existingData.active !== false ? 'checked' : ''}>
                                <label class="form-check-label" for="active${i}">
                                    Active (Include in dropdowns and reports)
                                </label>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            container.appendChild(quarterCard);
        }
    }

    async saveNetworkQuarters(networkName) {
        if (!this.networkConfig || !this.networkConfig[networkName]) {
            this.showAlert('Network not found', 'error');
            return;
        }

        const quarterCards = document.querySelectorAll('#editQuarterInputs .card');
        const quarters = [];
        let hasErrors = false;
        
        for (let i = 0; i < quarterCards.length; i++) {
            const card = quarterCards[i];
            const nameInput = card.querySelector('.quarter-name-input');
            const numberInput = card.querySelector('.quarter-number-input');
            const monthsInput = card.querySelector('.quarter-months-input');
            const activeInput = card.querySelector('.quarter-active-input');
            
            const name = nameInput.value.trim();
            const number = parseInt(numberInput.value.trim());
            const monthRange = monthsInput.value.trim();
            const active = activeInput.checked;
            
            // Validation
            if (!name) {
                this.showAlert(`Quarter ${i + 1}: Name is required`, 'error');
                nameInput.focus();
                hasErrors = true;
                break;
            }
            
            if (!number || number < 1) {
                this.showAlert(`Quarter ${i + 1}: Valid number is required`, 'error');
                numberInput.focus();
                hasErrors = true;
                break;
            }
            
            if (!monthRange) {
                this.showAlert(`Quarter ${i + 1}: Month range is required`, 'error');
                monthsInput.focus();
                hasErrors = true;
                break;
            }
            
            // Check for duplicate quarter numbers
            if (quarters.some(q => q.number === number)) {
                this.showAlert(`Quarter ${i + 1}: Duplicate quarter number ${number}`, 'error');
                numberInput.focus();
                hasErrors = true;
                break;
            }
            
            // Create quarter object
            const quarterObj = {
                name: name,
                number: number,
                monthRange: monthRange,
                active: active,
                displayName: `${name} (${monthRange})` // For backward compatibility
            };
            
            quarters.push(quarterObj);
        }
        
        if (hasErrors) {
            return;
        }

        // Update the network configuration
        this.networkConfig[networkName].quarters = quarters;
        
        // Update the global quarters array for backward compatibility
        this.quarters = this.quarters || [];
        quarters.forEach(quarter => {
            const displayName = quarter.displayName;
            if (!this.quarters.includes(displayName)) {
                this.quarters.push(displayName);
            }
        });
        
        // Remove old quarters that are no longer used
        const currentDisplayNames = quarters.map(q => q.displayName);
        this.quarters = this.quarters.filter(q => 
            currentDisplayNames.includes(q) || 
            !Object.values(this.networkConfig).some(config => 
                config.quarters && config.quarters.some(cq => 
                    (typeof cq === 'string' && cq === q) || 
                    (typeof cq === 'object' && cq.displayName === q)
                )
            )
        );

        // Save to backend
        await this.saveNetworkConfigToBackend();
        
        // Refresh the UI
        this.renderNetworksTable();
        this.populateUpdateForm();
        this.populateFilters();
        
        // Close the modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('quarterModal'));
        if (modal) {
            modal.hide();
        }
        
        this.showAlert(`Quarters updated successfully for ${networkName}`, 'success');
    }

    // Admin Functions
    async loadUsersList() {
        try {
            const data = await this.apiCall('/api/admin/users');
            if (data && Array.isArray(data)) {
                this.renderUsersTable(data);
            } else {
                console.log('No users data received');
            }
        } catch (error) {
            console.error('Error loading users:', error);
            this.showAlert('Failed to load users list', 'danger');
        }
    }

    renderUsersTable(users) {
        const tbody = document.getElementById('usersTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        users.forEach(user => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${user.username}</td>
                <td>${user.admin ? 'Admin' : 'User'}</td>
                <td>${user.active ? 'Active' : 'Inactive'}</td>
                <td>
                    <button class="btn btn-sm btn-warning" onclick="tracker.resetUserPassword('${user.username}')">
                        <i class="fas fa-key"></i> Reset Password
                    </button>
                    ${!user.admin ? `<button class="btn btn-sm btn-danger ms-2" onclick="tracker.deleteUser('${user.username}')">
                        <i class="fas fa-trash"></i> Delete
                    </button>` : ''}
                </td>
            `;
            tbody.appendChild(row);
        });
        
        // Also populate the reset password dropdown
        this.populateResetPasswordDropdown(users);
    }

    populateResetPasswordDropdown(users) {
        const dropdown = document.getElementById('resetUsername');
        if (!dropdown) return;
        
        dropdown.innerHTML = '<option value="">Select User</option>';
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.username;
            option.textContent = user.username;
            dropdown.appendChild(option);
        });
    }

    async addUser() {
        const username = document.getElementById('newUsername').value;
        const password = document.getElementById('newPassword').value;
        const role = document.getElementById('newUserRole').value;
        
        if (!username || !password || !role) {
            this.showAlert('Please fill in all required fields', 'warning');
            return;
        }
        
        try {
            const data = await this.apiCall('/api/admin/users', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password, role })
            });
            
            if (data && !data.error) {
                this.showAlert('User added successfully!', 'success');
                document.getElementById('addUserForm').reset();
                await this.loadUsersList();
            } else {
                this.showAlert('Failed to add user: ' + (data.message || 'Unknown error'), 'danger');
            }
        } catch (error) {
            this.showAlert('Failed to add user: ' + error.message, 'danger');
        }
    }

    async changeMyPassword() {
        const currentPassword = document.getElementById('currentPassword').value;
        const newPassword = document.getElementById('newPasswordSelf').value;
        const confirmNewPassword = document.getElementById('confirmPasswordSelf').value;
        
        if (!currentPassword || !newPassword || !confirmNewPassword) {
            this.showAlert('Please fill in all password fields', 'warning');
            return;
        }
        
        if (newPassword !== confirmNewPassword) {
            this.showAlert('New passwords do not match', 'warning');
            return;
        }
        
        try {
            const data = await this.apiCall('/api/user/change-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ currentPassword, newPassword })
            });
            
            if (data && !data.error) {
                this.showAlert('Password changed successfully!', 'success');
                document.getElementById('changeMyPasswordForm').reset();
            } else {
                this.showAlert('Failed to change password: ' + (data.message || 'Unknown error'), 'danger');
            }
        } catch (error) {
            this.showAlert('Failed to change password: ' + error.message, 'danger');
        }
    }

    async resetUserPassword() {
        const username = document.getElementById('resetUsername').value;
        const newPassword = document.getElementById('resetNewPassword').value;
        const confirmPassword = document.getElementById('resetConfirmPassword').value;
        
        if (!username || !newPassword || !confirmPassword) {
            this.showAlert('Please fill in all password fields', 'warning');
            return;
        }
        
        if (newPassword !== confirmPassword) {
            this.showAlert('Passwords do not match', 'warning');
            return;
        }
        
        try {
            const data = await this.apiCall(`/api/admin/users/${username}/reset-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ newPassword })
            });
            
            if (data && !data.error) {
                this.showAlert('User password reset successfully!', 'success');
                document.getElementById('resetUserPasswordForm').reset();
            } else {
                this.showAlert('Failed to reset password: ' + (data.message || 'Unknown error'), 'danger');
            }
        } catch (error) {
            this.showAlert('Failed to reset password: ' + error.message, 'danger');
        }
    }

    async deleteUser(username) {
        if (!confirm(`Are you sure you want to delete user "${username}"?`)) {
            return;
        }
        
        try {
            const data = await this.apiCall(`/api/admin/users/${username}`, {
                method: 'DELETE'
            });
            
            if (data && !data.error) {
                this.showAlert('User deleted successfully!', 'success');
                await this.loadUsersList();
            } else {
                this.showAlert('Failed to delete user: ' + (data.message || 'Unknown error'), 'danger');
            }
        } catch (error) {
            this.showAlert('Failed to delete user: ' + error.message, 'danger');
        }
    }
}

// Initialize the application
const tracker = new IOCBillTracker();

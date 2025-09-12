class Dashboard {
    constructor() {
        this.sidebar = document.getElementById('sidebar');
        this.toggleBtn = document.getElementById('toggleSidebar');
        this.navMenu = document.getElementById('navMenu');
        this.contentContainer = document.getElementById('contentContainer');
        this.pageTitle = document.getElementById('pageTitle');
        this.pageSubtitle = document.getElementById('pageSubtitle');
        this.themeToggle = document.getElementById('themeToggle');

        this.currentPage = 'dashboard';
        this.isCollapsed = false;

        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadPage('dashboard');
        this.setupResponsive();
    }

    setupEventListeners() {
        // Sidebar toggle
        this.toggleBtn.addEventListener('click', () => this.toggleSidebar());

        // Navigation menu
        this.navMenu.addEventListener('click', (e) => this.handleNavigation(e));

        // Theme toggle
        this.themeToggle.addEventListener('click', () => this.toggleTheme());

        // Responsive handling
        window.addEventListener('resize', () => this.handleResize());
    }

    toggleSidebar() {
        this.isCollapsed = !this.isCollapsed;
        this.sidebar.classList.toggle('collapsed', this.isCollapsed);

        // Update toggle icon
        const icon = this.toggleBtn.querySelector('i');
        icon.className = this.isCollapsed ? 'fas fa-bars' : 'fas fa-times';
    }

    handleNavigation(e) {
        e.preventDefault();

        const navItem = e.target.closest('.nav-item');
        if (!navItem) return;

        const page = navItem.dataset.page;
        if (page && page !== this.currentPage) {
            this.loadPage(page);
            this.setActiveNavItem(navItem);
        }
    }

    setActiveNavItem(activeItem) {
        // Remove active class from all items
        this.navMenu.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });

        // Add active class to clicked item
        activeItem.classList.add('active');
    }

    loadPage(page) {
        this.currentPage = page;
        this.updatePageHeader(page);

        // Add loading state
        this.contentContainer.classList.add('loading');

        // Check if this is an external HTML page
        const navItem = document.querySelector(`[data-page="${page}"]`);
        const externalPath = navItem?.dataset.external;

        if (externalPath) {
            // Load external HTML file
            this.loadExternalPage(externalPath);
        } else {
            // Load internal page content
            setTimeout(() => {
                this.contentContainer.innerHTML = this.getPageContent(page);
                this.contentContainer.classList.remove('loading');
                this.contentContainer.classList.add('fade-in');

                // Remove animation class after animation completes
                setTimeout(() => {
                    this.contentContainer.classList.remove('fade-in');
                }, 600);
            }, 300);
        }
    }

    async loadExternalPage(path) {
        try {
            const response = await fetch(path);
            if (!response.ok) {
                throw new Error(`Failed to load page: ${response.status}`);
            }

            const html = await response.text();

            // Parse the HTML and extract the body content
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const bodyContent = doc.body.innerHTML;

            // Load the content with transition
            setTimeout(() => {
                this.contentContainer.innerHTML = bodyContent;
                this.contentContainer.classList.remove('loading');
                this.contentContainer.classList.add('fade-in');

                // Remove animation class after animation completes
                setTimeout(() => {
                    this.contentContainer.classList.remove('fade-in');
                }, 600);
            }, 300);

        } catch (error) {
            console.error('Error loading external page:', error);

            // Show error message
            setTimeout(() => {
                this.contentContainer.innerHTML = `
                    <div class="error-container" style="text-align: center; padding: 3rem;">
                        <div style="font-size: 4rem; color: var(--error-color); margin-bottom: 1rem;">
                            <i class="fas fa-exclamation-triangle"></i>
                        </div>
                        <h2 style="font-size: 1.5rem; font-weight: 600; margin-bottom: 1rem; color: var(--text-primary);">Page Not Found</h2>
                        <p style="color: var(--text-secondary); margin-bottom: 2rem;">The requested page could not be loaded. Please check if the file exists.</p>
                        <button onclick="window.dashboard.loadPage('dashboard')" style="background: var(--primary-color); color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 0.5rem; cursor: pointer; font-weight: 500;">
                            <i class="fas fa-home" style="margin-right: 0.5rem;"></i>
                            Back to Dashboard
                        </button>
                    </div>
                `;
                this.contentContainer.classList.remove('loading');
                this.contentContainer.classList.add('fade-in');

                setTimeout(() => {
                    this.contentContainer.classList.remove('fade-in');
                }, 600);
            }, 300);
        }
    }

    updatePageHeader(page) {
        const pageConfig = this.getPageConfig(page);
        this.pageTitle.textContent = pageConfig.title;
        this.pageSubtitle.textContent = pageConfig.subtitle;
    }

    getPageConfig(page) {
        const configs = {
            dashboard: {
                title: 'Dashboard',
                subtitle: 'Welcome back! Here\'s what\'s happening.'
            },
            analytics: {
                title: 'Analytics',
                subtitle: 'Detailed insights and performance metrics.'
            },
            aicalls: {
                title: 'AI Calls',
                subtitle: 'AI agent conversation and settings'
            },
            projects: {
                title: 'Projects',
                subtitle: 'Manage your projects and track progress.'
            },
            team: {
                title: 'Team',
                subtitle: 'Collaborate with your team members.'
            },
            settings: {
                title: 'Settings',
                subtitle: 'Customize your workspace preferences.'
            },
            about: {
                title: 'About Us',
                subtitle: 'Learn more about our company and mission.'
            },
            contact: {
                title: 'Contact',
                subtitle: 'Get in touch with our team.'
            }
        };

        return configs[page] || configs.dashboard;
    }

    getPageContent(page) {
        const content = {
            dashboard: this.getDashboardContent(),
            analytics: this.getAnalyticsContent(),
            projects: this.getProjectsContent(),
            team: this.getTeamContent(),
            settings: this.getSettingsContent()
        };

        return content[page] || content.dashboard;
    }

    getDashboardContent() {
        return `
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Total Revenue</span>
                        <div class="stat-icon" style="background: linear-gradient(135deg, #3b82f6, #1e40af);">
                            <i class="fas fa-dollar-sign"></i>
                        </div>
                    </div>
                    <div class="stat-value">$24,780</div>
                    <div class="stat-change positive">
                        <i class="fas fa-arrow-up"></i>
                        <span>+12.5% from last month</span>
                    </div>
                </div>
                
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Active Users</span>
                        <div class="stat-icon" style="background: linear-gradient(135deg, #10b981, #059669);">
                            <i class="fas fa-users"></i>
                        </div>
                    </div>
                    <div class="stat-value">1,429</div>
                    <div class="stat-change positive">
                        <i class="fas fa-arrow-up"></i>
                        <span>+8.2% from last week</span>
                    </div>
                </div>
                
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Conversion Rate</span>
                        <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b, #d97706);">
                            <i class="fas fa-chart-line"></i>
                        </div>
                    </div>
                    <div class="stat-value">3.24%</div>
                    <div class="stat-change negative">
                        <i class="fas fa-arrow-down"></i>
                        <span>-2.1% from last month</span>
                    </div>
                </div>
                
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Total Orders</span>
                        <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6, #7c3aed);">
                            <i class="fas fa-shopping-cart"></i>
                        </div>
                    </div>
                    <div class="stat-value">892</div>
                    <div class="stat-change positive">
                        <i class="fas fa-arrow-up"></i>
                        <span>+15.3% from last week</span>
                    </div>
                </div>
            </div>
            
            <div class="chart-container">
                <div class="chart-header">
                    <h3 class="chart-title">Revenue Overview</h3>
                    <div class="chart-controls">
                        <button class="btn-secondary">Last 7 days</button>
                        <button class="btn-primary">Last 30 days</button>
                    </div>
                </div>
                <div class="chart-placeholder">
                    <div>
                        <i class="fas fa-chart-area" style="font-size: 3rem; margin-bottom: 1rem; opacity: 0.3;"></i>
                        <p>Chart visualization would go here</p>
                        <p style="font-size: 0.875rem; margin-top: 0.5rem;">Integrate with Chart.js, D3.js, or similar library</p>
                    </div>
                </div>
            </div>
            
            <div class="activity-container">
                <div class="activity-header">
                    <h3 class="activity-title">Recent Activity</h3>
                </div>
                <ul class="activity-list">
                    <li class="activity-item">
                        <div class="activity-icon" style="background: #3b82f6;">
                            <i class="fas fa-user-plus"></i>
                        </div>
                        <div class="activity-content">
                            <div class="activity-text">New user registered: Sarah Johnson</div>
                            <div class="activity-time">2 minutes ago</div>
                        </div>
                    </li>
                    <li class="activity-item">
                        <div class="activity-icon" style="background: #10b981;">
                            <i class="fas fa-check"></i>
                        </div>
                        <div class="activity-content">
                            <div class="activity-text">Order #1847 completed successfully</div>
                            <div class="activity-time">15 minutes ago</div>
                        </div>
                    </li>
                    <li class="activity-item">
                        <div class="activity-icon" style="background: #f59e0b;">
                            <i class="fas fa-exclamation-triangle"></i>
                        </div>
                        <div class="activity-content">
                            <div class="activity-text">Server maintenance scheduled for tonight</div>
                            <div class="activity-time">1 hour ago</div>
                        </div>
                    </li>
                    <li class="activity-item">
                        <div class="activity-icon" style="background: #8b5cf6;">
                            <i class="fas fa-upload"></i>
                        </div>
                        <div class="activity-content">
                            <div class="activity-text">New project files uploaded</div>
                            <div class="activity-time">3 hours ago</div>
                        </div>
                    </li>
                </ul>
            </div>
        `;
    }

    getAnalyticsContent() {
        return `
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Page Views</span>
                        <div class="stat-icon" style="background: linear-gradient(135deg, #3b82f6, #1e40af);">
                            <i class="fas fa-eye"></i>
                        </div>
                    </div>
                    <div class="stat-value">45,230</div>
                    <div class="stat-change positive">
                        <i class="fas fa-arrow-up"></i>
                        <span>+18.2% vs last period</span>
                    </div>
                </div>
                
                <div class="stat-card">
                    <div class="stat-header">
                        <span class="stat-title">Bounce Rate</span>
                        <div class="stat-icon" style="background: linear-gradient(135deg, #ef4444, #dc2626);">
                            <i class="fas fa-chart-pie"></i>
                        </div>
                    </div>
                    <div class="stat-value">32.4%</div>
                    <div class="stat-change negative">
                        <i class="fas fa-arrow-down"></i>
                        <span>-5.3% improvement</span>
                    </div>
                </div>
            </div>
            
            <div class="chart-container">
                <div class="chart-header">
                    <h3 class="chart-title">Traffic Analytics</h3>
                </div>
                <div class="chart-placeholder">
                    <div>
                        <i class="fas fa-chart-bar" style="font-size: 3rem; margin-bottom: 1rem; opacity: 0.3;"></i>
                        <p>Analytics charts and graphs would be displayed here</p>
                    </div>
                </div>
            </div>
        `;
    }

    getProjectsContent() {
        return `
            <div class="projects-header" style="margin-bottom: 2rem; display: flex; align-items: center; justify-content: space-between;">
                <h2 style="font-size: 1.5rem; font-weight: 600;">Active Projects</h2>
                <button class="btn-primary" style="background: var(--primary-color); color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 0.5rem; cursor: pointer; font-weight: 500;">
                    <i class="fas fa-plus" style="margin-right: 0.5rem;"></i>
                    New Project
                </button>
            </div>
            
            <div class="projects-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1.5rem;">
                <div class="project-card" style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 1rem; padding: 1.5rem; transition: var(--transition);">
                    <div class="project-header" style="margin-bottom: 1rem;">
                        <h3 style="font-size: 1.125rem; font-weight: 600; margin-bottom: 0.5rem;">E-commerce Platform</h3>
                        <span style="background: #10b981; color: white; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 500;">Active</span>
                    </div>
                    <p style="color: var(--text-secondary); margin-bottom: 1rem; font-size: 0.875rem;">Modern e-commerce solution with advanced features and analytics.</p>
                    <div class="project-progress" style="margin-bottom: 1rem;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem; font-size: 0.875rem;">
                            <span>Progress</span>
                            <span>78%</span>
                        </div>
                        <div style="width: 100%; height: 8px; background: var(--bg-tertiary); border-radius: 4px;">
                            <div style="width: 78%; height: 100%; background: var(--primary-color); border-radius: 4px;"></div>
                        </div>
                    </div>
                    <div class="project-team" style="display: flex; align-items: center; gap: 0.5rem;">
                        <span style="font-size: 0.875rem; color: var(--text-secondary);">Team:</span>
                        <div style="display: flex; gap: -0.5rem;">
                            <div style="width: 24px; height: 24px; background: var(--primary-color); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 0.75rem;">J</div>
                            <div style="width: 24px; height: 24px; background: var(--success-color); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 0.75rem;">S</div>
                            <div style="width: 24px; height: 24px; background: var(--accent-color); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 0.75rem;">M</div>
                        </div>
                    </div>
                </div>
                
                <div class="project-card" style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 1rem; padding: 1.5rem; transition: var(--transition);">
                    <div class="project-header" style="margin-bottom: 1rem;">
                        <h3 style="font-size: 1.125rem; font-weight: 600; margin-bottom: 0.5rem;">Mobile App Design</h3>
                        <span style="background: #f59e0b; color: white; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 500;">In Review</span>
                    </div>
                    <p style="color: var(--text-secondary); margin-bottom: 1rem; font-size: 0.875rem;">UI/UX design for iOS and Android mobile application.</p>
                    <div class="project-progress" style="margin-bottom: 1rem;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem; font-size: 0.875rem;">
                            <span>Progress</span>
                            <span>95%</span>
                        </div>
                        <div style="width: 100%; height: 8px; background: var(--bg-tertiary); border-radius: 4px;">
                            <div style="width: 95%; height: 100%; background: var(--accent-color); border-radius: 4px;"></div>
                        </div>
                    </div>
                    <div class="project-team" style="display: flex; align-items: center; gap: 0.5rem;">
                        <span style="font-size: 0.875rem; color: var(--text-secondary);">Team:</span>
                        <div style="display: flex; gap: -0.5rem;">
                            <div style="width: 24px; height: 24px; background: var(--primary-color); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 0.75rem;">A</div>
                            <div style="width: 24px; height: 24px; background: var(--success-color); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 0.75rem;">B</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    getTeamContent() {
        return `
            <div class="team-header" style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.5rem; font-weight: 600; margin-bottom: 0.5rem;">Team Members</h2>
                <p style="color: var(--text-secondary);">Manage your team and their permissions</p>
            </div>
            
            <div class="team-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem;">
                <div class="team-card" style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 1rem; padding: 1.5rem; text-align: center;">
                    <div class="member-avatar" style="width: 80px; height: 80px; background: var(--primary-color); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 2rem; margin: 0 auto 1rem;">
                        <i class="fas fa-user"></i>
                    </div>
                    <h3 style="font-size: 1.125rem; font-weight: 600; margin-bottom: 0.5rem;">John Doe</h3>
                    <p style="color: var(--text-secondary); margin-bottom: 0.5rem;">Product Manager</p>
                    <span style="background: var(--success-color); color: white; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem;">Active</span>
                    <div style="margin-top: 1rem; display: flex; gap: 0.5rem; justify-content: center;">
                        <button style="padding: 0.5rem; background: var(--bg-tertiary); border: none; border-radius: 0.5rem; color: var(--text-secondary); cursor: pointer;">
                            <i class="fas fa-envelope"></i>
                        </button>
                        <button style="padding: 0.5rem; background: var(--bg-tertiary); border: none; border-radius: 0.5rem; color: var(--text-secondary); cursor: pointer;">
                            <i class="fas fa-phone"></i>
                        </button>
                    </div>
                </div>
                
                <div class="team-card" style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 1rem; padding: 1.5rem; text-align: center;">
                    <div class="member-avatar" style="width: 80px; height: 80px; background: var(--accent-color); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 2rem; margin: 0 auto 1rem;">
                        <i class="fas fa-user"></i>
                    </div>
                    <h3 style="font-size: 1.125rem; font-weight: 600; margin-bottom: 0.5rem;">Sarah Wilson</h3>
                    <p style="color: var(--text-secondary); margin-bottom: 0.5rem;">UI/UX Designer</p>
                    <span style="background: var(--success-color); color: white; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem;">Active</span>
                    <div style="margin-top: 1rem; display: flex; gap: 0.5rem; justify-content: center;">
                        <button style="padding: 0.5rem; background: var(--bg-tertiary); border: none; border-radius: 0.5rem; color: var(--text-secondary); cursor: pointer;">
                            <i class="fas fa-envelope"></i>
                        </button>
                        <button style="padding: 0.5rem; background: var(--bg-tertiary); border: none; border-radius: 0.5rem; color: var(--text-secondary); cursor: pointer;">
                            <i class="fas fa-phone"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }

    getSettingsContent() {
        return `
            <div class="settings-container">
                <div class="settings-section" style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 1rem; padding: 1.5rem; margin-bottom: 1.5rem;">
                    <h3 style="font-size: 1.25rem; font-weight: 600; margin-bottom: 1rem;">General Settings</h3>
                    <div class="setting-item" style="display: flex; align-items: center; justify-content: space-between; padding: 1rem 0; border-bottom: 1px solid var(--border-color);">
                        <div>
                            <h4 style="font-weight: 500; margin-bottom: 0.25rem;">Email Notifications</h4>
                            <p style="color: var(--text-secondary); font-size: 0.875rem;">Receive email updates about your account</p>
                        </div>
                        <label class="toggle-switch" style="position: relative; display: inline-block; width: 50px; height: 24px;">
                            <input type="checkbox" checked style="opacity: 0; width: 0; height: 0;">
                            <span style="position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background: var(--primary-color); transition: 0.4s; border-radius: 34px; ::before { position: absolute; content: ''; height: 18px; width: 18px; left: 3px; bottom: 3px; background: white; transition: 0.4s; border-radius: 50%; }"></span>
                        </label>
                    </div>
                    <div class="setting-item" style="display: flex; align-items: center; justify-content: space-between; padding: 1rem 0; border-bottom: 1px solid var(--border-color);">
                        <div>
                            <h4 style="font-weight: 500; margin-bottom: 0.25rem;">Dark Mode</h4>
                            <p style="color: var(--text-secondary); font-size: 0.875rem;">Switch between light and dark themes</p>
                        </div>
                        <button class="theme-btn" style="background: var(--bg-tertiary); border: none; padding: 0.5rem 1rem; border-radius: 0.5rem; color: var(--text-primary); cursor: pointer;">
                            <i class="fas fa-moon"></i> Dark
                        </button>
                    </div>
                </div>
                
                <div class="settings-section" style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 1rem; padding: 1.5rem;">
                    <h3 style="font-size: 1.25rem; font-weight: 600; margin-bottom: 1rem;">Account Settings</h3>
                    <div class="setting-item" style="padding: 1rem 0; border-bottom: 1px solid var(--border-color);">
                        <h4 style="font-weight: 500; margin-bottom: 0.5rem;">Change Password</h4>
                        <button style="background: var(--primary-color); color: white; border: none; padding: 0.5rem 1rem; border-radius: 0.5rem; cursor: pointer;">Update Password</button>
                    </div>
                    <div class="setting-item" style="padding: 1rem 0;">
                        <h4 style="font-weight: 500; margin-bottom: 0.5rem;">Two-Factor Authentication</h4>
                        <p style="color: var(--text-secondary); font-size: 0.875rem; margin-bottom: 0.5rem;">Add an extra layer of security to your account</p>
                        <button style="background: var(--success-color); color: white; border: none; padding: 0.5rem 1rem; border-radius: 0.5rem; cursor: pointer;">Enable 2FA</button>
                    </div>
                </div>
            </div>
        `;
    }

    toggleTheme() {
        document.body.classList.toggle('light-theme');
        const icon = this.themeToggle.querySelector('i');

        if (document.body.classList.contains('light-theme')) {
            icon.className = 'fas fa-sun';
        } else {
            icon.className = 'fas fa-moon';
        }
    }

    setupResponsive() {
        this.handleResize();
    }

    handleResize() {
        if (window.innerWidth <= 768) {
            this.sidebar.classList.remove('collapsed');
            this.isCollapsed = false;
        }
    }
}

// Initialize dashboard when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.dashboard = new Dashboard();
});

// Add some interactive effects
document.addEventListener('DOMContentLoaded', () => {
    // Add hover effects to stat cards
    const statCards = document.querySelectorAll('.stat-card');
    statCards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-8px) scale(1.02)';
        });

        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
        });
    });

    // Add ripple effect to buttons
    const buttons = document.querySelectorAll('button, .nav-link');
    buttons.forEach(button => {
        button.addEventListener('click', function(e) {
            const ripple = document.createElement('span');
            const rect = this.getBoundingClientRect();
            const size = Math.max(rect.height, rect.width);
            const x = e.clientX - rect.left - size / 2;
            const y = e.clientY - rect.top - size / 2;

            ripple.style.width = ripple.style.height = size + 'px';
            ripple.style.left = x + 'px';
            ripple.style.top = y + 'px';
            ripple.classList.add('ripple-effect');

            // Add CSS for ripple effect
            ripple.style.position = 'absolute';
            ripple.style.borderRadius = '50%';
            ripple.style.background = 'rgba(255, 255, 255, 0.3)';
            ripple.style.transform = 'scale(0)';
            ripple.style.animation = 'ripple 0.6s linear';
            ripple.style.pointerEvents = 'none';

            this.appendChild(ripple);

            setTimeout(() => {
                ripple.remove();
            }, 600);
        });
    });
});

// Add CSS for ripple animation
const style = document.createElement('style');
style.textContent = `
    @keyframes ripple {
        to {
            transform: scale(4);
            opacity: 0;
        }
    }
    
    button, .nav-link {
        position: relative;
        overflow: hidden;
    }
`;
document.head.appendChild(style);
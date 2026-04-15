import { NavLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LayoutDashboard, Package, Search, Settings, ChevronLeft, ChevronRight } from 'lucide-react'
import './Sidebar.css'

interface SidebarProps {
  collapsed: boolean
  onToggle: () => void
}

interface NavItem {
  to: string
  icon: React.ReactNode
  labelKey: string
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', icon: <LayoutDashboard size={20} />, labelKey: 'nav.dashboard' },
  { to: '/dependencias', icon: <Package size={20} />, labelKey: 'nav.dependencies' },
  { to: '/busca', icon: <Search size={20} />, labelKey: 'nav.search' },
  { to: '/settings', icon: <Settings size={20} />, labelKey: 'nav.settings' },
]

function DashboardLogo() {
  return (
    <svg
      className="sidebar__logo"
      width="32"
      height="32"
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect width="32" height="32" rx="8" fill="#E24329" />
      <rect x="6" y="21" width="4" height="5" rx="1" fill="white" opacity="0.75" />
      <rect x="14" y="15" width="4" height="11" rx="1" fill="white" />
      <rect x="22" y="10" width="4" height="16" rx="1" fill="white" opacity="0.6" />
      <polyline
        points="8,21 16,15 24,10"
        stroke="white"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
        opacity="0.9"
      />
    </svg>
  )
}

export function Sidebar({ collapsed, onToggle }: SidebarProps) {
  const { t } = useTranslation()

  return (
    <aside className={`sidebar${collapsed ? ' sidebar--collapsed' : ''}`}>
      <div className="sidebar__header">
        {!collapsed && <DashboardLogo />}
        <button className="sidebar__toggle" onClick={onToggle} aria-label="Toggle sidebar">
          {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
      </div>

      <nav className="sidebar__nav">
        {NAV_ITEMS.map(({ to, icon, labelKey }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `sidebar__link${isActive ? ' sidebar__link--active' : ''}`
            }
            title={collapsed ? t(labelKey) : undefined}
          >
            <span className="sidebar__icon">{icon}</span>
            {!collapsed && <span className="sidebar__label">{t(labelKey)}</span>}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}

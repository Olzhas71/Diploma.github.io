import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ParkingSquare,
  Map as MapIcon,
  CalendarCheck,
  CreditCard,
  Car,
  BarChart3,
  Settings,
  Camera,
  LogOut,
  Menu,
  X,
  type LucideIcon,
} from 'lucide-react';
import clsx from 'clsx';
import { useAuthStore } from '@/store/auth';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import type { Role } from '@/types';

interface NavItem {
  to: string;
  labelKey: string;
  icon: LucideIcon;
  roles?: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { to: '/parkings',           labelKey: 'nav.parkings',      icon: ParkingSquare },
  { to: '/map',                labelKey: 'nav.map',           icon: MapIcon },
  { to: '/bookings',           labelKey: 'nav.bookings',      icon: CalendarCheck },
  { to: '/subscriptions',      labelKey: 'nav.subscriptions', icon: CreditCard },
  { to: '/vehicles',           labelKey: 'nav.vehicles',      icon: Car },
  { to: '/admin/stats',        labelKey: 'nav.stats',         icon: BarChart3, roles: ['ADMIN', 'OPERATOR'] },
  { to: '/admin/access-events', labelKey: 'nav.camera',       icon: Camera,    roles: ['ADMIN', 'OPERATOR'] },
  { to: '/admin/parkings',     labelKey: 'nav.admin',         icon: Settings,  roles: ['ADMIN', 'OPERATOR'] },
];

export function Layout() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { email, role, logout, accessToken } = useAuthStore();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const visible = NAV_ITEMS.filter((it) => !it.roles || (role && it.roles.includes(role)));

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    clsx(
      'flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-all',
      isActive
        ? 'bg-brand-50 text-brand-700 shadow-soft'
        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
    );

  return (
    <div className="min-h-full flex flex-col">
      <header className="bg-white/80 backdrop-blur-md border-b border-slate-200 sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between gap-4">
          <Link to="/" className="flex items-center gap-2 group">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 grid place-items-center shadow-soft group-hover:shadow-glow transition-shadow">
              <ParkingSquare className="w-5 h-5 text-white" />
            </div>
            <div className="leading-tight">
              <div className="font-bold text-slate-900">Parking</div>
              <div className="text-[10px] uppercase tracking-widest text-slate-500 -mt-0.5">{t('common.tagline')}</div>
            </div>
          </Link>

          {accessToken && (
            <nav className="hidden lg:flex items-center gap-1 flex-1 justify-center">
              {visible.map((it) => (
                <NavLink key={it.to} to={it.to} className={linkClass}>
                  <it.icon className="w-4 h-4" />
                  {t(it.labelKey)}
                </NavLink>
              ))}
            </nav>
          )}

          <div className="flex items-center gap-2">
            <LanguageSwitcher />
            {accessToken ? (
              <>
                <div className="hidden sm:flex items-center gap-2 pr-2 pl-3 py-1 rounded-full bg-slate-100">
                  <div className="w-7 h-7 rounded-full bg-gradient-to-br from-brand-500 to-accent-500 grid place-items-center text-xs font-bold text-white">
                    {email?.[0]?.toUpperCase() ?? '?'}
                  </div>
                  <div className="text-xs leading-tight">
                    <div className="font-semibold text-slate-700 truncate max-w-[140px]">{email}</div>
                    <div className="text-slate-500">{role}</div>
                  </div>
                </div>
                <button onClick={handleLogout} className="btn-ghost" title={t('nav.logout')}>
                  <LogOut className="w-4 h-4" />
                  <span className="hidden sm:inline">{t('nav.logout')}</span>
                </button>
                <button
                  onClick={() => setMobileOpen(!mobileOpen)}
                  className="lg:hidden btn-ghost p-2"
                  aria-label="Toggle menu"
                >
                  {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn-ghost">{t('nav.login')}</Link>
                <Link to="/register" className="btn-primary">{t('nav.register')}</Link>
              </>
            )}
          </div>
        </div>

        {accessToken && mobileOpen && (
          <div className="lg:hidden border-t border-slate-200 bg-white animate-fade-in-up">
            <nav className="max-w-7xl mx-auto px-4 py-3 grid grid-cols-2 gap-2">
              {visible.map((it) => (
                <NavLink
                  key={it.to}
                  to={it.to}
                  onClick={() => setMobileOpen(false)}
                  className={linkClass}
                >
                  <it.icon className="w-4 h-4" />
                  {t(it.labelKey)}
                </NavLink>
              ))}
            </nav>
          </div>
        )}
      </header>

      <main className="flex-1 grid-bg">
        <div className="max-w-7xl mx-auto p-4 md:p-8 animate-fade-in">
          <Outlet />
        </div>
      </main>

      <footer className="border-t border-slate-200 bg-white py-4 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex items-center justify-between flex-wrap gap-2">
          <span>{t('common.appName')} · v1.0</span>
          <span className="text-slate-400">Powered by Spring Boot 3 · React 18 · Tribuo ML</span>
        </div>
      </footer>
    </div>
  );
}

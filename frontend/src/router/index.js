import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      { path: '', redirect: '/hotels' },
      { path: 'hotels', name: 'hotels', component: () => import('../views/HotelsView.vue') },
      { path: 'hotels/:id', name: 'hotel-detail', component: () => import('../views/HotelDetailView.vue') },
      { path: 'rooms', name: 'rooms', component: () => import('../views/RoomsView.vue') },
      {
        path: 'booking/:typeId',
        name: 'booking',
        component: () => import('../views/BookingView.vue'),
        meta: { roles: ['MEMBER'] },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('../views/OrdersView.vue'),
        meta: { roles: ['MEMBER'] },
      },
      {
        path: 'orders/:orderNo',
        name: 'order-detail',
        component: () => import('../views/OrderDetailView.vue'),
      },
      {
        path: 'pms',
        name: 'pms',
        component: () => import('../views/PmsView.vue'),
        meta: { roles: ['RECEPTIONIST', 'ADMIN'] },
      },
      {
        path: 'admin',
        name: 'admin',
        component: () => import('../views/AdminView.vue'),
        meta: { roles: ['ADMIN'] },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/hotels' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return auth.isLoggedIn ? auth.defaultRoute : true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    return auth.defaultRoute
  }
  return true
})

export default router

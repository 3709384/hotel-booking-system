export const reservationStatusMap = {
  PENDING_PAYMENT: { label: '待支付', type: 'warning' },
  CONFIRMED: { label: '已确认', type: 'success' },
  CHECKED_IN: { label: '已入住', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  REFUNDING: { label: '退款中', type: 'warning' },
  REFUNDED: { label: '已退款', type: 'info' },
}

export const roomStatusMap = {
  VC: { label: '空闲可售', type: 'success' },
  VD: { label: '待清洁', type: 'warning' },
  OC: { label: '在住', type: 'primary' },
  OOO: { label: '维修停用', type: 'danger' },
}

export function formatMoney(value) {
  const number = Number(value || 0)
  return `¥${number.toFixed(2)}`
}

export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

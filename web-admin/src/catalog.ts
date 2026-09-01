export const vehicleModels = [
  { code: 'AF-SUV-PRO', label: 'AutoFlow SUV Pro' },
  { code: 'AF-SEDAN-X', label: 'AutoFlow Sedan X' },
  { code: 'AF-CITY-EV', label: 'AutoFlow City EV' },
] as const

export const modelCodes = vehicleModels.map(model => model.code)

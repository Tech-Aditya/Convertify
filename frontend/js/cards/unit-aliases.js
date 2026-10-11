/**
 * Extra words people search units by: plurals, British spellings and everyday abbreviations
 * that the catalog's name and symbol don't contain. Keyed by catalog unit code.
 */
export const UNIT_ALIASES = {
  // Length
  mm: 'millimetre millimetres millimeters', cm: 'centimetre centimetres centimeters',
  m: 'metre metres meters', km: 'kilometre kilometres kilometers kms kilo',
  in: 'inches', ft: 'feet', yd: 'yards', mi: 'miles', nmi: 'nautical miles nm sea',
  // Speed
  mps: 'metres per second meters', kmh: 'kph kmph kilometres per hour kilometers', mph: 'miles per hour',
  fps: 'feet per second', kn: 'knots nautical',
  // Time
  ms: 'milliseconds', s: 'seconds sec secs', min: 'minutes mins', h: 'hours hr hrs', d: 'days', wk: 'weeks',
  // Weight
  mg: 'milligrams milligramme', g: 'grams gramme grammes gm', kg: 'kilograms kilogramme kilos kgs kilo',
  t: 'tonnes ton tons metric ton', oz: 'ounces', lb: 'pounds lbs', st: 'stones',
  // Temperature
  C: 'centigrade celsius degrees', F: 'fahrenheit degrees', K: 'kelvin absolute',
  // Volume
  ml: 'millilitre millilitres milliliters cc', l: 'litre litres liters ltr', m3: 'cubic metre metres meters',
  in3: 'cubic inches', ft3: 'cubic feet', floz: 'fluid ounces', cup: 'cups', gal: 'gallons',
  impgal: 'imperial gallons uk gallon'
};

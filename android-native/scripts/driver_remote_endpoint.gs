const SHEETS = {
  drivers: 'drivers',
  cars: 'cars',
  mileageLogs: 'mileage_logs',
  config: 'config',
};

const DRIVER_HEADERS = [
  'login',
  'password',
  'name',
  'registration',
  'must_change_password',
  'last_mileage',
  'updated_at',
];

const CAR_HEADERS = [
  'registration',
  'driver_login',
  'driver_name',
  'last_mileage',
  'updated_at',
];

const MILEAGE_LOG_HEADERS = [
  'timestamp',
  'driver',
  'registration',
  'mileage',
  'request_id',
];

const CONFIG_HEADERS = ['key', 'value'];
const CACHE_TTL_SECONDS = 12;
const SCHEMA_VERSION = '2';

function doGet(e) {
  return handleRequest_(e);
}

function doPost(e) {
  return handleRequest_(e);
}

function handleRequest_(e) {
  try {
    const payload = normalizePayload_(e);
    const action = normalizeAction_(payload.action);

    ensureSchemaFast_();

    switch (action) {
      case 'health':
      case 'ping':
      case 'status':
        return jsonResponse_({ status: 'ok', message: 'driver endpoint ready', schema_version: SCHEMA_VERSION });

      case 'create_driver':
      case 'upsert_driver':
      case 'register_driver':
        return jsonResponse_(withWriteLock_(function() {
          return createOrUpdateDriver_(payload, { forcePasswordReset: true, allowCreate: true });
        }));

      case 'reset_driver':
        return jsonResponse_(withWriteLock_(function() {
          return createOrUpdateDriver_(payload, { forcePasswordReset: true, allowCreate: true });
        }));

      case 'change_password':
      case 'set_password':
        return jsonResponse_(withWriteLock_(function() {
          return changePassword_(payload);
        }));

      case 'sync_driver_assignment':
      case 'assign_driver':
      case 'link_driver':
      case 'set_driver_assignment':
        return jsonResponse_(withWriteLock_(function() {
          return syncDriverAssignment_(payload);
        }));

      case 'unassign_driver':
      case 'delete_driver':
      case 'remove_driver':
        return jsonResponse_(withWriteLock_(function() {
          return deleteDriver_(payload);
        }));

      case 'login_driver':
      case 'driver_login':
      case 'login':
        return jsonResponse_(loginDriver_(payload));

      case 'add_mileage':
      case 'save_mileage':
      case 'update_mileage':
      case 'mileage_update':
      case 'mileage':
        return jsonResponse_(withWriteLock_(function() {
          return saveMileage_(payload);
        }));

      case 'get_logs':
      case 'logs':
      case 'driver_logs':
      case 'mileage_logs':
        return jsonResponse_(getMileageLogs_(payload));

      case 'get_driver':
      case 'get_driver_by_login':
        return jsonResponse_(getDriverByLogin_(payload));

      case 'get_drivers':
        return jsonResponse_({ status: 'ok', drivers: getDriversSnapshot_(payload) });

      case 'get_cars':
        return jsonResponse_({ status: 'ok', cars: getCarsSnapshot_() });

      case 'get_state':
        return jsonResponse_({
          status: 'ok',
          drivers: getDriversSnapshot_(payload),
          cars: getCarsSnapshot_(),
          logs: getMileageLogs_(payload).logs,
        });

      case 'clear_all':
      case 'clear_test_data':
      case 'reset_all':
        return jsonResponse_(withWriteLock_(function() {
          return clearAllTestData_();
        }));

      default:
        return jsonResponse_({ status: 'error', message: action ? 'Unsupported action: ' + action : 'Missing action' });
    }
  } catch (error) {
    return jsonResponse_({ status: 'error', message: (error && error.message) ? error.message : String(error) });
  }
}

function normalizeAction_(action) {
  return String(action || '').trim().toLowerCase();
}

function withWriteLock_(callback) {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    return callback();
  } finally {
    lock.releaseLock();
  }
}

function ensureSchemaFast_() {
  const props = PropertiesService.getScriptProperties();
  const markerKey = 'schema_ready_v' + SCHEMA_VERSION;
  const marker = props.getProperty(markerKey);
  if (marker) return;

  ensureSheet_(SHEETS.drivers, DRIVER_HEADERS);
  ensureSheet_(SHEETS.cars, CAR_HEADERS);
  ensureSheet_(SHEETS.mileageLogs, MILEAGE_LOG_HEADERS);
  ensureSheet_(SHEETS.config, CONFIG_HEADERS);

  props.setProperty(markerKey, '1');
}

function ensureSheet_(name, headers) {
  const spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = spreadsheet.getSheetByName(name);
  if (!sheet) {
    sheet = spreadsheet.insertSheet(name);
  }

  const headerRange = sheet.getRange(1, 1, 1, headers.length);
  const currentHeaders = headerRange.getValues()[0];
  const needsHeader = headers.some(function(header, index) {
    return String(currentHeaders[index] || '').trim() !== header;
  });

  if (needsHeader) {
    headerRange.setValues([headers]);
    headerRange.setFontWeight('bold');
    sheet.setFrozenRows(1);
  }

  return sheet;
}

function normalizePayload_(e) {
  const fromBody = parseRequestBody_(e);
  const fromParams = parseParameters_(e);
  return Object.assign({}, fromParams, fromBody);
}

function parseRequestBody_(e) {
  if (!e || !e.postData || !e.postData.contents) return {};
  const raw = e.postData.contents;
  const trimmed = String(raw || '').trim();
  if (!trimmed) return {};

  try {
    return JSON.parse(trimmed);
  } catch (_jsonError) {
    const params = {};
    trimmed.split('&').forEach(function(part) {
      if (!part) return;
      const pair = part.split('=');
      const key = decodeURIComponent(String(pair[0] || '').replace(/\+/g, ' '));
      const value = decodeURIComponent(String(pair.slice(1).join('=') || '').replace(/\+/g, ' '));
      if (key) params[key] = value;
    });
    return params;
  }
}

function parseParameters_(e) {
  const result = {};
  if (!e || !e.parameter) return result;
  Object.keys(e.parameter).forEach(function(key) {
    result[key] = e.parameter[key];
  });
  return result;
}

function jsonResponse_(payload) {
  return ContentService.createTextOutput(JSON.stringify(payload)).setMimeType(ContentService.MimeType.JSON);
}

function nowText_() {
  return Utilities.formatDate(new Date(), Session.getScriptTimeZone() || 'Europe/Warsaw', 'yyyy-MM-dd HH:mm:ss');
}

function datePart_(timestamp) {
  const raw = String(timestamp || '').trim();
  if (!raw) return '';
  const direct = raw.match(/^(\d{4}-\d{2}-\d{2})/);
  if (direct && direct[1]) return direct[1];
  const date = new Date(raw);
  if (!isNaN(date.getTime())) {
    return Utilities.formatDate(date, Session.getScriptTimeZone() || 'Europe/Warsaw', 'yyyy-MM-dd');
  }
  return '';
}

function normalizeRegistration_(value) {
  return String(value || '').trim().toUpperCase();
}

function normalizeLogin_(value) {
  return String(value || '').trim();
}

function normalizeName_(value) {
  return String(value || '').trim();
}

function normalizeMileage_(value) {
  const parsed = Number(String(value || '').trim());
  if (!isFinite(parsed)) throw new Error('Mileage must be a number');
  return Math.max(0, Math.round(parsed));
}

function toBooleanFlag_(value) {
  if (value === true || value === 1) return 1;
  const normalized = String(value || '').trim().toLowerCase();
  return normalized === '1' || normalized === 'true' || normalized === 'yes' ? 1 : 0;
}

function generatePassword_(length) {
  const size = length || 6;
  let result = '';
  for (let i = 0; i < size; i += 1) {
    result += String(Math.floor(Math.random() * 10));
  }
  return result;
}

function cacheKeyForSheet_(sheetName) {
  return 'sheet_v' + SCHEMA_VERSION + '_' + sheetName;
}

function loadSheetRecordsCached_(sheetName) {
  const cache = CacheService.getScriptCache();
  const key = cacheKeyForSheet_(sheetName);
  const cached = cache.get(key);
  if (cached) {
    const parsed = JSON.parse(cached);
    parsed.sheet = ensureSheet_(sheetName, getHeadersForSheet_(sheetName));
    return parsed;
  }

  const fresh = loadSheetRecordsDirect_(sheetName);
  const snapshotForCache = { headers: fresh.headers, rows: fresh.rows };
  cache.put(key, JSON.stringify(snapshotForCache), CACHE_TTL_SECONDS);
  return fresh;
}

function invalidateSheetCache_(sheetName) {
  CacheService.getScriptCache().remove(cacheKeyForSheet_(sheetName));
}

function loadSheetRecordsDirect_(sheetName) {
  const sheet = ensureSheet_(sheetName, getHeadersForSheet_(sheetName));
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) {
    return { headers: getHeadersForSheet_(sheetName), rows: [], sheet: sheet };
  }

  const headers = values[0].map(function(value) {
    return String(value || '').trim();
  });

  const rows = values.slice(1).map(function(row, index) {
    const record = { _rowNumber: index + 2 };
    headers.forEach(function(header, headerIndex) {
      record[header] = row[headerIndex];
    });
    return record;
  });

  return { headers: headers, rows: rows, sheet: sheet };
}

function clearAllTestData_() {
  clearSheetRows_(SHEETS.drivers, DRIVER_HEADERS);
  clearSheetRows_(SHEETS.cars, CAR_HEADERS);
  clearSheetRows_(SHEETS.mileageLogs, MILEAGE_LOG_HEADERS);
  clearSheetRows_(SHEETS.config, CONFIG_HEADERS);
  return { status: 'ok', message: 'All test data cleared' };
}

function clearSheetRows_(sheetName, headers) {
  const sheet = ensureSheet_(sheetName, headers);
  const lastRow = sheet.getLastRow();
  const lastColumn = Math.max(sheet.getLastColumn(), headers.length);
  if (lastRow > 1) {
    sheet.getRange(2, 1, lastRow - 1, lastColumn).clearContent();
  }
  invalidateSheetCache_(sheetName);
}

function getHeadersForSheet_(sheetName) {
  switch (sheetName) {
    case SHEETS.drivers: return DRIVER_HEADERS;
    case SHEETS.cars: return CAR_HEADERS;
    case SHEETS.mileageLogs: return MILEAGE_LOG_HEADERS;
    case SHEETS.config: return CONFIG_HEADERS;
    default: throw new Error('Unknown sheet: ' + sheetName);
  }
}

function upsertDriverRecord_(driver) {
  const snapshot = loadSheetRecordsDirect_(SHEETS.drivers);
  const normalizedRegistration = normalizeRegistration_(driver.registration);
  const normalizedLogin = normalizeLogin_(driver.login).toLowerCase();

  const rowIndex = snapshot.rows.findIndex(function(row) {
    return normalizeLogin_(row.login).toLowerCase() === normalizedLogin &&
      normalizeRegistration_(row.registration) === normalizedRegistration;
  });

  const rowValues = [
    driver.login,
    driver.password,
    driver.name,
    driver.registration,
    driver.must_change_password,
    driver.last_mileage,
    driver.updated_at,
  ];

  if (rowIndex >= 0) {
    snapshot.sheet.getRange(snapshot.rows[rowIndex]._rowNumber, 1, 1, rowValues.length).setValues([rowValues]);
    invalidateSheetCache_(SHEETS.drivers);
    return snapshot.rows[rowIndex]._rowNumber;
  }

  snapshot.sheet.appendRow(rowValues);
  invalidateSheetCache_(SHEETS.drivers);
  return snapshot.sheet.getLastRow();
}

function upsertCarRecord_(car) {
  const snapshot = loadSheetRecordsDirect_(SHEETS.cars);
  const rowIndex = snapshot.rows.findIndex(function(row) {
    return normalizeRegistration_(row.registration) === normalizeRegistration_(car.registration);
  });

  const rowValues = [
    car.registration,
    car.driver_login,
    car.driver_name,
    car.last_mileage,
    car.updated_at,
  ];

  if (rowIndex >= 0) {
    snapshot.sheet.getRange(snapshot.rows[rowIndex]._rowNumber, 1, 1, rowValues.length).setValues([rowValues]);
    invalidateSheetCache_(SHEETS.cars);
    return snapshot.rows[rowIndex]._rowNumber;
  }

  snapshot.sheet.appendRow(rowValues);
  invalidateSheetCache_(SHEETS.cars);
  return snapshot.sheet.getLastRow();
}

function findDriversByLogin_(login) {
  const normalizedLogin = normalizeLogin_(login).toLowerCase();
  if (!normalizedLogin) return [];
  const snapshot = loadSheetRecordsCached_(SHEETS.drivers);
  return snapshot.rows.filter(function(row) {
    return normalizeLogin_(row.login).toLowerCase() === normalizedLogin;
  });
}

function findDriverByRegistration_(registration) {
  const normalizedRegistration = normalizeRegistration_(registration);
  const snapshot = loadSheetRecordsCached_(SHEETS.drivers);
  return snapshot.rows.find(function(row) {
    return normalizeRegistration_(row.registration) === normalizedRegistration;
  }) || null;
}

function findDriverByLoginAndRegistration_(login, registration) {
  const normalizedLogin = normalizeLogin_(login).toLowerCase();
  const normalizedRegistration = normalizeRegistration_(registration);
  const snapshot = loadSheetRecordsCached_(SHEETS.drivers);
  return snapshot.rows.find(function(row) {
    return normalizeLogin_(row.login).toLowerCase() === normalizedLogin &&
      normalizeRegistration_(row.registration) === normalizedRegistration;
  }) || null;
}

function findCarByRegistration_(registration) {
  const normalizedRegistration = normalizeRegistration_(registration);
  const snapshot = loadSheetRecordsCached_(SHEETS.cars);
  return snapshot.rows.find(function(row) {
    return normalizeRegistration_(row.registration) === normalizedRegistration;
  }) || null;
}

function createOrUpdateDriver_(payload, options) {
  const login = normalizeLogin_(payload.login);
  const registration = normalizeRegistration_(payload.registration);
  const name = normalizeName_(payload.name || payload.driverName || payload.driver);
  if (!login) throw new Error('Missing login');
  if (!registration) throw new Error('Missing registration');

  const current = findDriverByLoginAndRegistration_(login, registration) || findDriverByRegistration_(registration);
  if (!current && options && options.allowCreate === false) {
    throw new Error('Driver not found');
  }

  const password = String(payload.password || '').trim() || (current ? String(current.password || '').trim() : generatePassword_(6));
  const mustChangePassword = (options && options.forcePasswordReset)
    ? 1
    : toBooleanFlag_(payload.must_change_password || (current ? current.must_change_password : 0));

  const driver = {
    login: login,
    password: password,
    name: name || normalizeName_(current && current.name),
    registration: registration,
    must_change_password: mustChangePassword,
    last_mileage: Number((current && current.last_mileage) || 0) || 0,
    updated_at: nowText_(),
  };

  upsertDriverRecord_(driver);
  syncDriverAssignment_({ login: login, name: driver.name, registration: registration });

  return {
    status: 'ok',
    login: driver.login,
    password: driver.password,
    name: driver.name,
    registration: driver.registration,
    change_password: driver.must_change_password,
    message: current ? 'Driver updated' : 'Driver created',
  };
}

function syncDriverAssignment_(payload) {
  const login = normalizeLogin_(payload.login || payload.driver_login);
  const registration = normalizeRegistration_(payload.registration || payload.plate || payload.rej);
  const name = normalizeName_(payload.name || payload.driverName || payload.driver);
  if (!login) throw new Error('Missing login');
  if (!registration) throw new Error('Missing registration');

  const sameLoginDrivers = findDriversByLogin_(login);
  const exact = findDriverByLoginAndRegistration_(login, registration);
  const existingOnRegistration = findDriverByRegistration_(registration);
  const credentialsSource = exact || sameLoginDrivers[0] || existingOnRegistration;
  if (!credentialsSource) throw new Error('Driver not found for assignment');

  upsertDriverRecord_({
    login: login,
    password: String(credentialsSource.password || ''),
    name: name || normalizeName_(credentialsSource.name),
    registration: registration,
    must_change_password: toBooleanFlag_(credentialsSource.must_change_password),
    last_mileage: Number(credentialsSource.last_mileage || 0) || 0,
    updated_at: nowText_(),
  });

  const car = findCarByRegistration_(registration);
  upsertCarRecord_({
    registration: registration,
    driver_login: login,
    driver_name: name || normalizeName_(credentialsSource.name),
    last_mileage: Number((car && car.last_mileage) || (credentialsSource && credentialsSource.last_mileage) || 0) || 0,
    updated_at: nowText_(),
  });

  return {
    status: 'ok',
    login: login,
    registration: registration,
    name: name || normalizeName_(credentialsSource.name),
    message: 'Driver assignment synchronized',
  };
}

function deleteDriver_(payload) {
  const registration = normalizeRegistration_(payload.registration || payload.plate || payload.rej);
  const login = normalizeLogin_(payload.login || payload.driver_login);
  if (!registration) throw new Error('Missing registration');

  const snapshot = loadSheetRecordsDirect_(SHEETS.drivers);
  const rowsToDetach = snapshot.rows.filter(function(row) {
    const sameRegistration = normalizeRegistration_(row.registration) === registration;
    if (!sameRegistration) return false;
    if (!login) return true;
    return normalizeLogin_(row.login).toLowerCase() === login.toLowerCase();
  });

  rowsToDetach.forEach(function(row) {
    upsertDriverRecord_({
      login: normalizeLogin_(row.login),
      password: String(row.password || ''),
      name: normalizeName_(row.name),
      registration: '',
      must_change_password: toBooleanFlag_(row.must_change_password),
      last_mileage: Number(row.last_mileage || 0) || 0,
      updated_at: nowText_(),
    });
  });

  const remaining = loadSheetRecordsCached_(SHEETS.drivers).rows.filter(function(row) {
    return normalizeRegistration_(row.registration) === registration;
  });
  const primary = remaining[0] || null;
  const car = findCarByRegistration_(registration);

  upsertCarRecord_({
    registration: registration,
    driver_login: primary ? normalizeLogin_(primary.login) : '',
    driver_name: primary ? normalizeName_(primary.name) : '',
    last_mileage: Number((car && car.last_mileage) || 0) || 0,
    updated_at: nowText_(),
  });

  return {
    status: 'ok',
    registration: registration,
    login: login,
    message: 'Driver unassigned',
  };
}

function changePassword_(payload) {
  const login = normalizeLogin_(payload.login);
  const password = String(payload.password || '').trim();
  if (!login) throw new Error('Missing login');
  if (!password) throw new Error('Missing password');

  const drivers = findDriversByLogin_(login);
  if (!drivers.length) throw new Error('Driver not found');

  drivers.forEach(function(driver) {
    upsertDriverRecord_({
      login: normalizeLogin_(driver.login),
      password: password,
      name: normalizeName_(driver.name),
      registration: normalizeRegistration_(driver.registration),
      must_change_password: 0,
      last_mileage: Number(driver.last_mileage || 0) || 0,
      updated_at: nowText_(),
    });
  });

  return { status: 'ok', login: login, message: 'Password changed' };
}

function loginDriver_(payload) {
  const login = normalizeLogin_(payload.login);
  const password = String(payload.password || '').trim();
  const requestedRegistration = normalizeRegistration_(payload.registration || payload.plate || payload.rej);
  if (!login) throw new Error('Missing login');
  if (!password) throw new Error('Missing password');

  const drivers = findDriversByLogin_(login).filter(function(row) {
    return String(row.password || '').trim() === password;
  });
  if (!drivers.length) throw new Error('Invalid login or password');

  const chosen = drivers.find(function(row) {
    return requestedRegistration && normalizeRegistration_(row.registration) === requestedRegistration;
  }) || drivers.find(function(row) {
    return normalizeRegistration_(row.registration);
  }) || drivers[0];

  const registrations = drivers.map(function(row) {
    return normalizeRegistration_(row.registration);
  }).filter(function(reg) {
    return !!reg;
  }).filter(function(value, index, arr) {
    return arr.indexOf(value) === index;
  }).sort();

  return {
    status: 'ok',
    login: normalizeLogin_(chosen.login),
    name: normalizeName_(chosen.name),
    registration: normalizeRegistration_(chosen.registration),
    registrations: registrations,
    change_password: toBooleanFlag_(chosen.must_change_password),
  };
}

function saveMileage_(payload) {
  const rawLogin = normalizeLogin_(payload.login || payload.driver_login || payload.driver);
  const requestedRegistration = normalizeRegistration_(payload.registration || payload.plate || payload.rej);
  const explicitDriverName = normalizeName_(payload.name || payload.driverName);
  const requestId = String(payload.request_id || payload.requestId || '').trim();

  const drivers = rawLogin ? findDriversByLogin_(rawLogin) : [];
  const registration = requestedRegistration || normalizeRegistration_(drivers[0] && drivers[0].registration);
  if (!registration) throw new Error('Missing registration');

  if (drivers.length) {
    const hasRegistration = drivers.some(function(row) {
      return normalizeRegistration_(row.registration) === registration;
    });
    if (!hasRegistration) {
      throw new Error('Driver is not assigned to this vehicle');
    }
  }

  const mileage = normalizeMileage_(payload.mileage || payload.value || payload.odometer);
  const car = findCarByRegistration_(registration);
  const matchingDriver = drivers.find(function(row) {
    return normalizeRegistration_(row.registration) === registration;
  }) || null;

  const highestKnownMileage = Math.max(
    Number((matchingDriver && matchingDriver.last_mileage) || 0) || 0,
    Number((car && car.last_mileage) || 0) || 0
  );
  if (mileage < highestKnownMileage) {
    throw new Error('Przebieg mniejszy niż ostatni, sprawdź wprowadzone dane');
  }

  const timestamp = String(payload.timestamp || '').trim() || nowText_();
  const dayKey = datePart_(timestamp) || datePart_(nowText_());
  const driverIdentity = rawLogin || explicitDriverName || normalizeName_(matchingDriver && matchingDriver.name);
  if (!driverIdentity) throw new Error('Missing driver identity');

  const dayConflict = findMileageLogForDay_(registration, dayKey);
  if (dayConflict) {
    const sameDriver = String(dayConflict.driver || '').trim().toLowerCase() === driverIdentity.toLowerCase();
    throw new Error(sameDriver
      ? 'Przebieg dla tego auta został już dziś zapisany'
      : 'Inny kierowca już dzisiaj wpisał przebieg dla tego auta');
  }

  const alreadyStored = hasMileageLog_(timestamp, driverIdentity, registration, mileage, requestId);
  const logSheet = ensureSheet_(SHEETS.mileageLogs, MILEAGE_LOG_HEADERS);
  if (!alreadyStored) {
    logSheet.appendRow([timestamp, driverIdentity, registration, mileage, requestId]);
    invalidateSheetCache_(SHEETS.mileageLogs);
  }

  upsertCarRecord_({
    registration: registration,
    driver_login: rawLogin || normalizeLogin_(car && car.driver_login),
    driver_name: explicitDriverName || normalizeName_(matchingDriver && matchingDriver.name) || normalizeName_(car && car.driver_name),
    last_mileage: mileage,
    updated_at: nowText_(),
  });

  if (matchingDriver) {
    upsertDriverRecord_({
      login: normalizeLogin_(matchingDriver.login),
      password: String(matchingDriver.password || ''),
      name: normalizeName_(matchingDriver.name),
      registration: registration,
      must_change_password: toBooleanFlag_(matchingDriver.must_change_password),
      last_mileage: mileage,
      updated_at: nowText_(),
    });
  }

  return {
    status: 'ok',
    registration: registration,
    mileage: mileage,
    timestamp: timestamp,
    driver: driverIdentity,
    duplicate: alreadyStored,
    request_id: requestId,
    message: alreadyStored ? 'Mileage already stored' : 'Mileage saved',
  };
}

function findMileageLogForDay_(registration, dayKey) {
  const targetRegistration = normalizeRegistration_(registration);
  if (!targetRegistration || !dayKey) return null;

  const snapshot = loadSheetRecordsCached_(SHEETS.mileageLogs);
  for (let i = 0; i < snapshot.rows.length; i += 1) {
    const row = snapshot.rows[i];
    if (datePart_(row.timestamp) === dayKey && normalizeRegistration_(row.registration) === targetRegistration) {
      return row;
    }
  }
  return null;
}

function hasMileageLog_(timestamp, driver, registration, mileage, requestId) {
  const snapshot = loadSheetRecordsCached_(SHEETS.mileageLogs);
  return snapshot.rows.some(function(row) {
    if (requestId && String(row.request_id || '').trim() && String(row.request_id || '').trim() === requestId) {
      return true;
    }
    return String(row.timestamp || '').trim() === String(timestamp || '').trim() &&
      String(row.driver || '').trim() === String(driver || '').trim() &&
      normalizeRegistration_(row.registration) === normalizeRegistration_(registration) &&
      (Number(row.mileage || 0) || 0) === mileage;
  });
}

function getMileageLogs_(payload) {
  const snapshot = loadSheetRecordsCached_(SHEETS.mileageLogs);
  const limit = Math.max(1, Math.min(5000, Number(payload && payload.limit ? payload.limit : 500) || 500));
  const rows = [MILEAGE_LOG_HEADERS];

  const start = Math.max(0, snapshot.rows.length - limit);
  for (let i = start; i < snapshot.rows.length; i += 1) {
    const row = snapshot.rows[i];
    rows.push([
      String(row.timestamp || ''),
      String(row.driver || ''),
      normalizeRegistration_(row.registration),
      Number(row.mileage || 0) || 0,
      String(row.request_id || ''),
    ]);
  }

  return { status: 'ok', logs: rows };
}

function getDriverByLogin_(payload) {
  const login = normalizeLogin_(payload.login);
  const requestedRegistration = normalizeRegistration_(payload.registration || payload.plate || payload.rej);
  if (!login) throw new Error('Missing login');

  const drivers = findDriversByLogin_(login);
  const selected = drivers.find(function(row) {
    return requestedRegistration && normalizeRegistration_(row.registration) === requestedRegistration;
  }) || drivers[0] || null;

  if (!selected) {
    return { status: 'error', message: 'Driver not found' };
  }

  return {
    status: 'ok',
    login: normalizeLogin_(selected.login),
    password: String(selected.password || ''),
    name: normalizeName_(selected.name),
    registration: normalizeRegistration_(selected.registration),
    change_password: toBooleanFlag_(selected.must_change_password),
    last_mileage: Number(selected.last_mileage || 0) || 0,
    updated_at: String(selected.updated_at || ''),
  };
}

function getDriversSnapshot_(payload) {
  const loginFilter = normalizeLogin_(payload && payload.login);
  const registrationFilter = normalizeRegistration_(payload && (payload.registration || payload.plate || payload.rej));

  return loadSheetRecordsCached_(SHEETS.drivers).rows
    .filter(function(row) {
      if (loginFilter && normalizeLogin_(row.login).toLowerCase() !== loginFilter.toLowerCase()) return false;
      if (registrationFilter && normalizeRegistration_(row.registration) !== registrationFilter) return false;
      return true;
    })
    .map(function(row) {
      return {
        login: normalizeLogin_(row.login),
        password: String(row.password || ''),
        name: normalizeName_(row.name),
        registration: normalizeRegistration_(row.registration),
        must_change_password: toBooleanFlag_(row.must_change_password),
        last_mileage: Number(row.last_mileage || 0) || 0,
        updated_at: String(row.updated_at || ''),
      };
    });
}

function getCarsSnapshot_() {
  return loadSheetRecordsCached_(SHEETS.cars).rows.map(function(row) {
    return {
      registration: normalizeRegistration_(row.registration),
      driver_login: normalizeLogin_(row.driver_login),
      driver_name: normalizeName_(row.driver_name),
      last_mileage: Number(row.last_mileage || 0) || 0,
      updated_at: String(row.updated_at || ''),
    };
  });
}

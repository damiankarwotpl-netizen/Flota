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
];

const CONFIG_HEADERS = ['key', 'value'];

function doGet(e) {
  return handleRequest_(e);
}

function doPost(e) {
  return handleRequest_(e);
}

function handleRequest_(e) {
  try {
    const payload = normalizePayload_(e);
    const action = String(payload.action || '').trim();

    ensureSchema_();

    switch (action) {
      case 'health':
      case 'ping':
        return jsonResponse_({ status: 'ok', message: 'driver endpoint ready' });

      case 'create_driver':
        return jsonResponse_(createOrUpdateDriver_(payload, { forcePasswordReset: true, allowCreate: true }));

      case 'reset_driver':
        return jsonResponse_(createOrUpdateDriver_(payload, { forcePasswordReset: true, allowCreate: true }));

      case 'change_password':
        return jsonResponse_(changePassword_(payload));

      case 'sync_driver_assignment':
        return jsonResponse_(syncDriverAssignment_(payload));

      case 'delete_driver':
        return jsonResponse_(deleteDriver_(payload));

      case 'login_driver':
      case 'driver_login':
      case 'login':
        return jsonResponse_(loginDriver_(payload));

      case 'add_mileage':
      case 'save_mileage':
      case 'update_mileage':
      case 'mileage_update':
        return jsonResponse_(saveMileage_(payload));

      case 'get_logs':
      case 'logs':
      case 'driver_logs':
        return jsonResponse_(getMileageLogs_());

      case 'get_driver':
      case 'get_driver_by_login':
        return jsonResponse_(getDriverByLogin_(payload));

      case 'get_drivers':
        return jsonResponse_({ status: 'ok', drivers: getDriversSnapshot_() });

      case 'get_cars':
        return jsonResponse_({ status: 'ok', cars: getCarsSnapshot_() });

      case 'get_state':
        return jsonResponse_({
          status: 'ok',
          drivers: getDriversSnapshot_(),
          cars: getCarsSnapshot_(),
          logs: getMileageLogs_().logs,
        });

      default:
        return jsonResponse_({
          status: 'error',
          message: action ? 'Unsupported action: ' + action : 'Missing action',
        });
    }
  } catch (error) {
    return jsonResponse_({
      status: 'error',
      message: error && error.message ? error.message : String(error),
    });
  }
}

function ensureSchema_() {
  ensureSheet_(SHEETS.drivers, DRIVER_HEADERS);
  ensureSheet_(SHEETS.cars, CAR_HEADERS);
  ensureSheet_(SHEETS.mileageLogs, MILEAGE_LOG_HEADERS);
  ensureSheet_(SHEETS.config, CONFIG_HEADERS);
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
  if (!raw) return {};

  const trimmed = String(raw).trim();
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
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

function nowText_() {
  return Utilities.formatDate(new Date(), Session.getScriptTimeZone() || 'Europe/Warsaw', 'yyyy-MM-dd HH:mm:ss');
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

function getSheetRecords_(sheetName) {
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

function getHeadersForSheet_(sheetName) {
  switch (sheetName) {
    case SHEETS.drivers:
      return DRIVER_HEADERS;
    case SHEETS.cars:
      return CAR_HEADERS;
    case SHEETS.mileageLogs:
      return MILEAGE_LOG_HEADERS;
    case SHEETS.config:
      return CONFIG_HEADERS;
    default:
      throw new Error('Unknown sheet: ' + sheetName);
  }
}

function upsertDriverRecord_(driver) {
  const snapshot = getSheetRecords_(SHEETS.drivers);
  const rowIndex = snapshot.rows.findIndex(function(row) {
    return normalizeLogin_(row.login).toLowerCase() === normalizeLogin_(driver.login).toLowerCase();
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
    return snapshot.rows[rowIndex]._rowNumber;
  }

  snapshot.sheet.appendRow(rowValues);
  return snapshot.sheet.getLastRow();
}

function upsertCarRecord_(car) {
  const snapshot = getSheetRecords_(SHEETS.cars);
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
    return snapshot.rows[rowIndex]._rowNumber;
  }

  snapshot.sheet.appendRow(rowValues);
  return snapshot.sheet.getLastRow();
}

function findDriverByLogin_(login) {
  const normalizedLogin = normalizeLogin_(login).toLowerCase();
  const snapshot = getSheetRecords_(SHEETS.drivers);
  for (let i = 0; i < snapshot.rows.length; i += 1) {
    if (normalizeLogin_(snapshot.rows[i].login).toLowerCase() === normalizedLogin) {
      return snapshot.rows[i];
    }
  }
  return null;
}

function findDriverByRegistration_(registration) {
  const normalizedRegistration = normalizeRegistration_(registration);
  const snapshot = getSheetRecords_(SHEETS.drivers);
  for (let i = 0; i < snapshot.rows.length; i += 1) {
    if (normalizeRegistration_(snapshot.rows[i].registration) === normalizedRegistration) {
      return snapshot.rows[i];
    }
  }
  return null;
}

function findCarByRegistration_(registration) {
  const normalizedRegistration = normalizeRegistration_(registration);
  const snapshot = getSheetRecords_(SHEETS.cars);
  for (let i = 0; i < snapshot.rows.length; i += 1) {
    if (normalizeRegistration_(snapshot.rows[i].registration) === normalizedRegistration) {
      return snapshot.rows[i];
    }
  }
  return null;
}

function createOrUpdateDriver_(payload, options) {
  const login = normalizeLogin_(payload.login);
  const registration = normalizeRegistration_(payload.registration);
  const name = normalizeName_(payload.name);
  if (!login) throw new Error('Missing login');
  if (!registration) throw new Error('Missing registration');

  const current = findDriverByLogin_(login);
  if (!current && options && options.allowCreate === false) {
    throw new Error('Driver not found');
  }

  const password = String(payload.password || '').trim() || (current ? String(current.password || '').trim() : generatePassword_(6));
  const mustChangePassword = (options && options.forcePasswordReset) ? 1 : toBooleanFlag_(payload.must_change_password || (current ? current.must_change_password : 0));
  const lastMileage = current && String(current.last_mileage || '').trim() ? Number(current.last_mileage) : 0;

  const driver = {
    login: login,
    password: password,
    name: name || normalizeName_(current && current.name),
    registration: registration,
    must_change_password: mustChangePassword,
    last_mileage: isFinite(lastMileage) ? Math.max(0, lastMileage) : 0,
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
  const login = normalizeLogin_(payload.login);
  const registration = normalizeRegistration_(payload.registration);
  const name = normalizeName_(payload.name);
  if (!login) throw new Error('Missing login');
  if (!registration) throw new Error('Missing registration');

  const driver = findDriverByLogin_(login);
  if (!driver) {
    throw new Error('Driver not found for assignment');
  }

  const driversSnapshot = getSheetRecords_(SHEETS.drivers);
  driversSnapshot.rows.forEach(function(row) {
    const rowLogin = normalizeLogin_(row.login).toLowerCase();
    const rowRegistration = normalizeRegistration_(row.registration);

    if (rowLogin === login.toLowerCase()) {
      const currentName = name || normalizeName_(row.name);
      upsertDriverRecord_({
        login: login,
        password: String(row.password || ''),
        name: currentName,
        registration: registration,
        must_change_password: toBooleanFlag_(row.must_change_password),
        last_mileage: Number(row.last_mileage || 0) || 0,
        updated_at: nowText_(),
      });
      return;
    }

    if (rowRegistration === registration) {
      upsertDriverRecord_({
        login: String(row.login || ''),
        password: String(row.password || ''),
        name: normalizeName_(row.name),
        registration: '',
        must_change_password: toBooleanFlag_(row.must_change_password),
        last_mileage: Number(row.last_mileage || 0) || 0,
        updated_at: nowText_(),
      });
    }
  });

  const carsSnapshot = getSheetRecords_(SHEETS.cars);
  carsSnapshot.rows.forEach(function(row) {
    const rowRegistration = normalizeRegistration_(row.registration);
    const rowDriverLogin = normalizeLogin_(row.driver_login).toLowerCase();

    if (rowRegistration === registration || rowDriverLogin === login.toLowerCase()) {
      upsertCarRecord_({
        registration: rowRegistration,
        driver_login: rowRegistration === registration ? login : '',
        driver_name: rowRegistration === registration ? (name || normalizeName_(driver.name)) : '',
        last_mileage: Number(row.last_mileage || 0) || 0,
        updated_at: nowText_(),
      });
    }
  });

  if (!findCarByRegistration_(registration)) {
    upsertCarRecord_({
      registration: registration,
      driver_login: login,
      driver_name: name || normalizeName_(driver.name),
      last_mileage: Number(driver.last_mileage || 0) || 0,
      updated_at: nowText_(),
    });
  }

  return {
    status: 'ok',
    login: login,
    registration: registration,
    name: name || normalizeName_(driver.name),
    message: 'Driver assignment synchronized',
  };
}

function deleteDriver_(payload) {
  const registration = normalizeRegistration_(payload.registration);
  if (!registration) throw new Error('Missing registration');

  const driversSnapshot = getSheetRecords_(SHEETS.drivers);
  driversSnapshot.rows.forEach(function(row) {
    if (normalizeRegistration_(row.registration) === registration) {
      upsertDriverRecord_({
        login: String(row.login || ''),
        password: String(row.password || ''),
        name: normalizeName_(row.name),
        registration: '',
        must_change_password: toBooleanFlag_(row.must_change_password),
        last_mileage: Number(row.last_mileage || 0) || 0,
        updated_at: nowText_(),
      });
    }
  });

  const carsSnapshot = getSheetRecords_(SHEETS.cars);
  carsSnapshot.rows.forEach(function(row) {
    if (normalizeRegistration_(row.registration) === registration) {
      upsertCarRecord_({
        registration: registration,
        driver_login: '',
        driver_name: '',
        last_mileage: Number(row.last_mileage || 0) || 0,
        updated_at: nowText_(),
      });
    }
  });

  return {
    status: 'ok',
    registration: registration,
    message: 'Driver deleted/unassigned',
  };
}

function changePassword_(payload) {
  const login = normalizeLogin_(payload.login);
  const password = String(payload.password || '').trim();
  if (!login) throw new Error('Missing login');
  if (!password) throw new Error('Missing password');

  const driver = findDriverByLogin_(login);
  if (!driver) throw new Error('Driver not found');

  upsertDriverRecord_({
    login: login,
    password: password,
    name: normalizeName_(driver.name),
    registration: normalizeRegistration_(driver.registration),
    must_change_password: 0,
    last_mileage: Number(driver.last_mileage || 0) || 0,
    updated_at: nowText_(),
  });

  return {
    status: 'ok',
    login: login,
    message: 'Password changed',
  };
}

function loginDriver_(payload) {
  const login = normalizeLogin_(payload.login);
  const password = String(payload.password || '').trim();
  if (!login) throw new Error('Missing login');
  if (!password) throw new Error('Missing password');

  const driver = findDriverByLogin_(login);
  if (!driver) throw new Error('Invalid login or password');
  if (String(driver.password || '').trim() !== password) throw new Error('Invalid login or password');

  return {
    status: 'ok',
    login: normalizeLogin_(driver.login),
    name: normalizeName_(driver.name),
    registration: normalizeRegistration_(driver.registration),
    change_password: toBooleanFlag_(driver.must_change_password),
  };
}

function saveMileage_(payload) {
  const rawLogin = normalizeLogin_(payload.login || payload.driver_login || payload.driver);
  const driver = rawLogin ? findDriverByLogin_(rawLogin) : null;

  const requestedRegistration = normalizeRegistration_(payload.registration || payload.plate || payload.rej);
  const registration = requestedRegistration || normalizeRegistration_(driver && driver.registration);
  if (!registration) throw new Error('Missing registration');

  if (driver) {
    const assignedRegistration = normalizeRegistration_(driver.registration);
    if (assignedRegistration && assignedRegistration !== registration) {
      throw new Error('Driver is not assigned to this vehicle');
    }
  }

  const mileage = normalizeMileage_(payload.mileage || payload.value || payload.odometer);
  const timestamp = String(payload.timestamp || '').trim() || nowText_();
  const driverIdentity = rawLogin || normalizeName_(payload.driver || payload.name || payload.driverName);
  if (!driverIdentity) throw new Error('Missing driver identity');

  const alreadyStored = hasMileageLog_(timestamp, driverIdentity, registration, mileage);
  const logSheet = ensureSheet_(SHEETS.mileageLogs, MILEAGE_LOG_HEADERS);
  if (!alreadyStored) {
    logSheet.appendRow([timestamp, driverIdentity, registration, mileage]);
  }

  const car = findCarByRegistration_(registration);
  upsertCarRecord_({
    registration: registration,
    driver_login: rawLogin || normalizeLogin_(car && car.driver_login),
    driver_name: normalizeName_(payload.name || payload.driverName || (driver && driver.name) || (car && car.driver_name)),
    last_mileage: mileage,
    updated_at: nowText_(),
  });

  if (driver) {
    upsertDriverRecord_({
      login: normalizeLogin_(driver.login),
      password: String(driver.password || ''),
      name: normalizeName_(driver.name),
      registration: normalizeRegistration_(driver.registration),
      must_change_password: toBooleanFlag_(driver.must_change_password),
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
    message: alreadyStored ? 'Mileage already stored' : 'Mileage saved',
  };
}

function hasMileageLog_(timestamp, driver, registration, mileage) {
  const snapshot = getSheetRecords_(SHEETS.mileageLogs);
  return snapshot.rows.some(function(row) {
    return String(row.timestamp || '').trim() === String(timestamp || '').trim() &&
      String(row.driver || '').trim() === String(driver || '').trim() &&
      normalizeRegistration_(row.registration) === normalizeRegistration_(registration) &&
      (Number(row.mileage || 0) || 0) === mileage;
  });
}

function getMileageLogs_() {
  const snapshot = getSheetRecords_(SHEETS.mileageLogs);
  const rows = [MILEAGE_LOG_HEADERS];
  snapshot.rows.forEach(function(row) {
    rows.push([
      String(row.timestamp || ''),
      String(row.driver || ''),
      normalizeRegistration_(row.registration),
      Number(row.mileage || 0) || 0,
    ]);
  });

  return {
    status: 'ok',
    logs: rows,
  };
}

function getDriverByLogin_(payload) {
  const login = normalizeLogin_(payload.login);
  if (!login) throw new Error('Missing login');

  const driver = findDriverByLogin_(login);
  if (!driver) {
    return {
      status: 'error',
      message: 'Driver not found',
    };
  }

  return {
    status: 'ok',
    login: normalizeLogin_(driver.login),
    password: String(driver.password || ''),
    name: normalizeName_(driver.name),
    registration: normalizeRegistration_(driver.registration),
    change_password: toBooleanFlag_(driver.must_change_password),
    last_mileage: Number(driver.last_mileage || 0) || 0,
    updated_at: String(driver.updated_at || ''),
  };
}

function getDriversSnapshot_() {
  const snapshot = getSheetRecords_(SHEETS.drivers);
  return snapshot.rows.map(function(row) {
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
  const snapshot = getSheetRecords_(SHEETS.cars);
  return snapshot.rows.map(function(row) {
    return {
      registration: normalizeRegistration_(row.registration),
      driver_login: normalizeLogin_(row.driver_login),
      driver_name: normalizeName_(row.driver_name),
      last_mileage: Number(row.last_mileage || 0) || 0,
      updated_at: String(row.updated_at || ''),
    };
  });
}

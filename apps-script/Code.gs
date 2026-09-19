// Deploy target: the "Daily Tracker" tab, identified by gid so renaming the tab doesn't break this.
const SHEET_GID = 833906007;

// Column numbers (1-indexed) matching the sheet layout:
// Date | Breakfast | Lunch | Dinner | Snacks | Travel | Other Expenses | Total | Total Spent Till Date
const CATEGORY_COLUMNS = {
  breakfast: 2,
  lunch: 3,
  dinner: 4,
  snacks: 5,
  travel: 6,
  otherExpenses: 7,
};
const TOTAL_COLUMN = 8;
const TOTAL_SPENT_TILL_DATE_COLUMN = 9;

// Receives one category's delta ({displayDate, category, amount}) and does a read-add-write
// against the real row, then recomputes that row's Total and the month-to-date running total
// from the sheet's own history — the caller never needs to know that history itself.
function doPost(e) {
  const data = JSON.parse(e.postData.contents);
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheets()
    .find(s => s.getSheetId() === SHEET_GID);

  const rowIndex = findRowForDate(sheet, data.displayDate);
  sheet.getRange(rowIndex, 1).setValue(data.displayDate);

  const categoryColumn = CATEGORY_COLUMNS[data.category];
  const currentValue = Number(sheet.getRange(rowIndex, categoryColumn).getValue()) || 0;
  sheet.getRange(rowIndex, categoryColumn).setValue(currentValue + data.amount);

  const rowTotal = Object.values(CATEGORY_COLUMNS)
    .map(col => Number(sheet.getRange(rowIndex, col).getValue()) || 0)
    .reduce((a, b) => a + b, 0);
  sheet.getRange(rowIndex, TOTAL_COLUMN).setValue(rowTotal);

  const monthToDate = computeMonthToDateTotal(sheet, rowIndex, rowTotal);
  sheet.getRange(rowIndex, TOTAL_SPENT_TILL_DATE_COLUMN).setValue(monthToDate);

  return ContentService.createTextOutput(JSON.stringify({ ok: true, row: rowIndex, total: rowTotal, monthToDate }))
    .setMimeType(ContentService.MimeType.JSON);
}

// Reuses today's row if it already has data; otherwise uses the first blank row
// (the sheet is pre-formatted with empty rows for the whole year).
function findRowForDate(sheet, displayDate) {
  const dates = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1).getValues();
  let firstBlankRow = -1;
  for (let i = 0; i < dates.length; i++) {
    const cellValue = dates[i][0];
    if (cellValue === displayDate) return i + 2;
    if (!cellValue && firstBlankRow === -1) firstBlankRow = i + 2;
  }
  return firstBlankRow !== -1 ? firstBlankRow : sheet.getLastRow() + 1;
}

// Sums the Total column from the start of the current month through rowIndex, scanning
// backward until the month name changes or a blank date cell is hit. This is recomputed from
// scratch every time rather than chained from the previous row's stored value, since the sheet
// has months of history where that column was never filled in. currentRowTotal is passed in
// directly rather than read back from the sheet, since Sheets batches writes internally and a
// getValue() right after setValue() on the same row can still return the pre-write value.
function computeMonthToDateTotal(sheet, rowIndex, currentRowTotal) {
  const currentMonth = extractMonth(sheet.getRange(rowIndex, 1).getValue());
  let sum = currentRowTotal;
  for (let r = rowIndex - 1; r >= 2; r--) {
    const dateValue = sheet.getRange(r, 1).getValue();
    if (!dateValue || extractMonth(dateValue) !== currentMonth) break;
    sum += Number(sheet.getRange(r, TOTAL_COLUMN).getValue()) || 0;
  }
  return sum;
}

function extractMonth(displayDate) {
  const parts = String(displayDate).trim().split(/\s+/);
  return parts[parts.length - 1];
}

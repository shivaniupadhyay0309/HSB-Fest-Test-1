/**
 * Zenith '26 College Fest Admin & Operations Dashboard Logic
 */

let allEventsCache = [];
let pendingDeleteRegId = null;

const CATEGORY_COLORS = {
  Technical: 'bg-blue-50 text-blue-700 border-blue-200',
  Cultural: 'bg-purple-50 text-purple-700 border-purple-200',
  Sports: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  Gaming: 'bg-rose-50 text-rose-700 border-rose-200',
  Workshop: 'bg-amber-50 text-amber-700 border-amber-200',
  Default: 'bg-slate-50 text-slate-700 border-slate-200'
};

document.addEventListener('DOMContentLoaded', () => {
  loadDashboardData();
});

async function loadDashboardData() {
  hideAdminError();
  await Promise.all([
    fetchStats(),
    fetchAdminEvents(),
    fetchRegistrations()
  ]);
}

/**
 * Step 8.1 & 8.2: Fetch and render Stats and Category Breakdown
 */
async function fetchStats() {
  try {
    const res = await fetch('/api/stats');
    if (!res.ok) throw new Error(`Stats returned ${res.status}`);
    const stats = await res.json();

    document.getElementById('stat-total-events').textContent = stats.total_events;
    document.getElementById('stat-total-registrations').textContent = stats.total_registrations;
    document.getElementById('stat-utilization-pct').textContent = `${stats.overall_utilization_pct}%`;
    document.getElementById('stat-capacity-subtext').textContent =
      `${stats.total_registrations} / ${stats.total_capacity} total fest seats`;
    document.getElementById('stat-upcoming-events').textContent = stats.upcoming_events_count;

    renderCategoryBreakdown(stats.events_by_category, stats.total_events);
  } catch (err) {
    console.error('Failed to fetch stats:', err);
    showAdminError(`Could not load stats: ${err.message}`);
  }
}

function renderCategoryBreakdown(breakdown, totalEvents) {
  const container = document.getElementById('category-breakdown-container');
  if (!container) return;
  container.innerHTML = '';

  const categories = Object.keys(breakdown || {});
  if (categories.length === 0) {
    container.innerHTML = '<div class="text-xs text-slate-400">No categories found.</div>';
    return;
  }

  categories.forEach((cat) => {
    const count = breakdown[cat];
    const pct = totalEvents > 0 ? Math.round((count / totalEvents) * 100) : 0;
    const badgeColor = CATEGORY_COLORS[cat] || CATEGORY_COLORS.Default;

    const box = document.createElement('div');
    box.className = `p-3.5 rounded-xl border ${badgeColor} flex flex-col justify-between`;
    box.innerHTML = `
      <div class="flex items-center justify-between">
        <span class="text-xs font-bold uppercase tracking-wider">${cat}</span>
        <span class="text-base font-black">${count}</span>
      </div>
      <div class="mt-2 text-[11px] opacity-75 font-medium">
        ${pct}% of all events
      </div>
    `;
    container.appendChild(box);
  });
}

/**
 * Step 8.3 & 8.4: Fetch events and render table sorted by utilization % descending
 */
async function fetchAdminEvents() {
  const tbody = document.getElementById('events-table-body');
  try {
    const res = await fetch('/api/events');
    if (!res.ok) throw new Error(`Events returned ${res.status}`);
    const events = await res.json();
    allEventsCache = events;

    populateEventFilterDropdown(events);

    // Compute utilization % for each event and sort descending
    const eventsWithStats = events.map((ev) => {
      const registered = Math.max(0, ev.capacity - ev.available_seats);
      const utilPct = ev.capacity > 0 ? Math.round((registered / ev.capacity) * 1000) / 10 : 0.0;
      return {
        ...ev,
        registered,
        utilization_pct: utilPct
      };
    });

    // Sort by utilization percentage descending
    eventsWithStats.sort((a, b) => b.utilization_pct - a.utilization_pct);

    tbody.innerHTML = '';
    if (eventsWithStats.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center py-6 text-slate-400">No events found.</td></tr>';
      return;
    }

    eventsWithStats.forEach((ev) => {
      const isHighDemand = ev.utilization_pct >= 90.0;
      const row = document.createElement('tr');
      row.className = isHighDemand
        ? 'bg-rose-50/70 hover:bg-rose-100/70 transition-colors'
        : 'hover:bg-slate-50 transition-colors';

      row.innerHTML = `
        <td class="py-3 px-4 font-semibold text-slate-900 flex items-center space-x-2">
          <span class="text-lg">${ev.icon || '⚡'}</span>
          <span>${escapeHtml(ev.title)}</span>
          ${isHighDemand ? '<span class="ml-2 px-1.5 py-0.5 rounded text-[10px] font-black uppercase tracking-wider bg-rose-600 text-white">🔥 High Demand</span>' : ''}
        </td>
        <td class="py-3 px-3">
          <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border ${CATEGORY_COLORS[ev.category] || CATEGORY_COLORS.Default}">
            ${ev.category}
          </span>
        </td>
        <td class="py-3 px-3 text-slate-600">${ev.event_date} <span class="text-slate-400">•</span> ${ev.event_time}</td>
        <td class="py-3 px-3 text-slate-600 truncate max-w-[140px]">${escapeHtml(ev.venue)}</td>
        <td class="py-3 px-3 text-center font-medium">
          <span class="font-bold ${isHighDemand ? 'text-rose-700' : 'text-slate-800'}">${ev.registered}</span>
          <span class="text-slate-400">/ ${ev.capacity}</span>
        </td>
        <td class="py-3 px-4 text-right">
          <div class="inline-flex items-center gap-2">
            <div class="w-16 bg-slate-200 rounded-full h-2 overflow-hidden hidden sm:block">
              <div class="h-2 rounded-full ${isHighDemand ? 'bg-rose-600' : 'bg-indigo-600'}" style="width: ${Math.min(100, ev.utilization_pct)}%"></div>
            </div>
            <span class="font-bold text-xs ${isHighDemand ? 'text-rose-700' : 'text-slate-700'}">${ev.utilization_pct}%</span>
          </div>
        </td>
      `;
      tbody.appendChild(row);
    });
  } catch (err) {
    console.error('Failed to load events table:', err);
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-6 text-rose-500">Error loading events: ${err.message}</td></tr>`;
  }
}

function populateEventFilterDropdown(events) {
  const select = document.getElementById('event-filter-select');
  if (!select) return;
  const currentVal = select.value;
  select.innerHTML = '<option value="">All Events</option>';
  events.forEach((ev) => {
    const opt = document.createElement('option');
    opt.value = ev.id;
    opt.textContent = `${ev.icon || ''} ${ev.title}`;
    select.appendChild(opt);
  });
  if (currentVal) select.value = currentVal;
}

/**
 * Step 8.5: Fetch Registrations ledger
 */
async function fetchRegistrations(eventId = null) {
  const tbody = document.getElementById('registrations-table-body');
  try {
    let url = '/api/registrations';
    if (eventId) {
      url += `?event_id=${eventId}`;
    }
    const res = await fetch(url);
    if (!res.ok) throw new Error(`Registrations returned ${res.status}`);
    const registrations = await res.json();

    tbody.innerHTML = '';
    if (registrations.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center py-8 text-slate-400">No student registrations found for this selection.</td></tr>';
      return;
    }

    registrations.forEach((reg) => {
      const row = document.createElement('tr');
      row.className = 'hover:bg-slate-50 transition-colors';
      row.innerHTML = `
        <td class="py-3 px-4 font-mono text-xs text-slate-400">#${reg.id}</td>
        <td class="py-3 px-4 font-semibold text-slate-900">${escapeHtml(reg.student_name)}</td>
        <td class="py-3 px-3 font-mono text-xs text-slate-700">${escapeHtml(reg.roll_number)}</td>
        <td class="py-3 px-3 text-slate-600">${escapeHtml(reg.email)}${reg.phone ? `<span class="block text-[10px] text-slate-400">${escapeHtml(reg.phone)}</span>` : ''}</td>
        <td class="py-3 px-3 font-medium text-slate-800">${escapeHtml(reg.event_title)}</td>
        <td class="py-3 px-3 text-xs text-slate-500 whitespace-nowrap">${escapeHtml(reg.registered_at)}</td>
        <td class="py-3 px-4 text-center">
          <button
            onclick="promptCancelRegistration(${reg.id}, '${escapeJs(reg.student_name)}', '${escapeJs(reg.event_title)}')"
            class="px-2 py-1 text-[11px] font-semibold text-rose-600 hover:text-white hover:bg-rose-600 border border-rose-200 rounded-md transition shadow-xs"
            title="Cancel Registration"
          >
            Cancel
          </button>
        </td>
      `;
      tbody.appendChild(row);
    });
  } catch (err) {
    console.error('Failed to load registrations:', err);
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-6 text-rose-500">Error loading registrations: ${err.message}</td></tr>`;
  }
}

function filterRegistrationsByEvent(eventId) {
  fetchRegistrations(eventId ? parseInt(eventId, 10) : null);
}

/**
 * Stretch Goal 2: Cancel Registration with confirmation
 */
function promptCancelRegistration(regId, studentName, eventTitle) {
  pendingDeleteRegId = regId;
  const modal = document.getElementById('delete-reg-modal');
  const textEl = document.getElementById('delete-reg-confirm-text');
  textEl.innerHTML = `Cancel registration <strong>#${regId}</strong> for <strong>${escapeHtml(studentName)}</strong> in <em>${escapeHtml(eventTitle)}</em>? This will immediately free up the seat.`;
  modal.classList.remove('hidden');
}

function closeDeleteModal() {
  document.getElementById('delete-reg-modal').classList.add('hidden');
  pendingDeleteRegId = null;
}

async function executeDeleteRegistration() {
  if (!pendingDeleteRegId) return;
  const btn = document.getElementById('confirm-delete-btn');
  btn.disabled = true;
  btn.textContent = 'Cancelling...';

  try {
    const res = await fetch(`/api/registrations/${pendingDeleteRegId}`, {
      method: 'DELETE'
    });
    if (!res.ok) {
      const errData = await res.json();
      throw new Error(errData.detail || 'Failed to cancel registration');
    }

    closeDeleteModal();
    // Refresh all data immediately
    await loadDashboardData();
  } catch (err) {
    alert(`Error cancelling registration: ${err.message}`);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Confirm Cancel';
  }
}

/**
 * Stretch Goal 1: Add New Event Modal
 */
function openAddEventModal() {
  document.getElementById('add-event-form').reset();
  const alertEl = document.getElementById('add-event-alert');
  alertEl.classList.add('hidden');
  document.getElementById('add-event-modal').classList.remove('hidden');
}

function closeAddEventModal() {
  document.getElementById('add-event-modal').classList.add('hidden');
}

async function handleAddEventSubmit(e) {
  e.preventDefault();
  const alertEl = document.getElementById('add-event-alert');
  const submitBtn = document.getElementById('add-event-btn');

  const title = document.getElementById('new-title').value.trim();
  const category = document.getElementById('new-category').value;
  const icon = document.getElementById('new-icon').value.trim() || '⚡';
  const event_date = document.getElementById('new-date').value.trim();
  const event_time = document.getElementById('new-time').value.trim();
  const venue = document.getElementById('new-venue').value.trim();
  const capacity = parseInt(document.getElementById('new-capacity').value, 10);
  const fee = parseInt(document.getElementById('new-fee').value, 10) || 0;
  const description = document.getElementById('new-description').value.trim();

  if (!title || !venue || isNaN(capacity) || capacity <= 0) {
    alertEl.className = 'p-3 rounded-xl text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200';
    alertEl.textContent = 'Please fill all required fields and ensure capacity > 0.';
    alertEl.classList.remove('hidden');
    return;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = 'Creating...';

  try {
    const res = await fetch('/api/events', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title,
        category,
        icon,
        event_date,
        event_time,
        venue,
        capacity,
        fee,
        description
      })
    });

    const data = await res.json();
    if (res.status === 201) {
      alertEl.className = 'p-3 rounded-xl text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200';
      alertEl.textContent = `Event "${title}" created successfully!`;
      alertEl.classList.remove('hidden');

      setTimeout(() => {
        closeAddEventModal();
        loadDashboardData();
      }, 1000);
    } else {
      alertEl.className = 'p-3 rounded-xl text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200';
      alertEl.textContent = data.detail || 'Failed to create event.';
      alertEl.classList.remove('hidden');
    }
  } catch (err) {
    alertEl.className = 'p-3 rounded-xl text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200';
    alertEl.textContent = `Error: ${err.message}`;
    alertEl.classList.remove('hidden');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Create Event';
  }
}

/**
 * Tab Switching: Events Table vs Registrations Table
 */
function switchTab(tab) {
  const eventsSec = document.getElementById('section-events-table');
  const regSec = document.getElementById('section-registrations-table');
  const btnEvents = document.getElementById('tab-btn-events');
  const btnReg = document.getElementById('tab-btn-registrations');

  if (tab === 'events') {
    eventsSec.classList.remove('hidden');
    regSec.classList.add('hidden');
    btnEvents.className = 'pb-3 border-b-2 border-indigo-600 text-indigo-600 transition';
    btnReg.className = 'pb-3 border-b-2 border-transparent text-slate-500 hover:text-slate-800 transition';
  } else {
    eventsSec.classList.add('hidden');
    regSec.classList.remove('hidden');
    btnReg.className = 'pb-3 border-b-2 border-indigo-600 text-indigo-600 transition';
    btnEvents.className = 'pb-3 border-b-2 border-transparent text-slate-500 hover:text-slate-800 transition';
  }
}

function showAdminError(msg) {
  const banner = document.getElementById('admin-error-banner');
  const text = document.getElementById('admin-error-text');
  if (banner && text) {
    text.textContent = msg;
    banner.classList.remove('hidden');
  }
}

function hideAdminError() {
  const banner = document.getElementById('admin-error-banner');
  if (banner) banner.classList.add('hidden');
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function escapeJs(text) {
  if (!text) return '';
  return String(text).replace(/'/g, "\\'").replace(/"/g, '\\"');
}

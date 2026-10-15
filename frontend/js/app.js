/**
 * Zenith '26 College Fest Portal — Student Portal Logic
 */

let currentCategory = 'All';
let currentSearch = '';
let searchDebounceTimer = null;
let currentModalEvent = null;

// Category badge color mapping
const CATEGORY_COLORS = {
  Technical: 'bg-blue-100 text-blue-800 border-blue-200',
  Cultural: 'bg-purple-100 text-purple-800 border-purple-200',
  Sports: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  Gaming: 'bg-rose-100 text-rose-800 border-rose-200',
  Workshop: 'bg-amber-100 text-amber-800 border-amber-200',
  Default: 'bg-slate-100 text-slate-800 border-slate-200'
};

document.addEventListener('DOMContentLoaded', () => {
  initEventListeners();
  fetchEvents();
});

function initEventListeners() {
  // Category buttons
  const categoryContainer = document.getElementById('category-filters');
  if (categoryContainer) {
    categoryContainer.addEventListener('click', (e) => {
      const btn = e.target.closest('.filter-btn');
      if (!btn) return;
      const category = btn.getAttribute('data-category');
      if (category === currentCategory) return;

      currentCategory = category;
      updateCategoryButtonStyles();
      fetchEvents();
    });
  }

  // Search input with debounce
  const searchInput = document.getElementById('search-input');
  const clearBtn = document.getElementById('search-clear-btn');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      clearTimeout(searchDebounceTimer);
      const val = e.target.value.trim();
      if (clearBtn) {
        clearBtn.classList.toggle('hidden', val.length === 0);
      }
      searchDebounceTimer = setTimeout(() => {
        currentSearch = val;
        fetchEvents();
      }, 300);
    });
  }

  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      if (searchInput) searchInput.value = '';
      clearBtn.classList.add('hidden');
      currentSearch = '';
      fetchEvents();
    });
  }

  // Modal close events
  const closeBtn = document.getElementById('modal-close-btn');
  const backdrop = document.getElementById('modal-backdrop');
  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  if (backdrop) {
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) closeModal();
    });
  }

  // ESC key to close modal
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !backdrop.classList.contains('hidden')) {
      closeModal();
    }
  });

  // Registration form submission
  const regForm = document.getElementById('registration-form');
  if (regForm) {
    regForm.addEventListener('submit', handleRegistrationSubmit);
  }
}

function updateCategoryButtonStyles() {
  document.querySelectorAll('.filter-btn').forEach((btn) => {
    const cat = btn.getAttribute('data-category');
    if (cat === currentCategory) {
      btn.classList.add('active-category');
      btn.classList.remove('inactive-category');
    } else {
      btn.classList.remove('active-category');
      btn.classList.add('inactive-category');
    }
  });
}

function resetFilters() {
  currentCategory = 'All';
  currentSearch = '';
  const searchInput = document.getElementById('search-input');
  const clearBtn = document.getElementById('search-clear-btn');
  if (searchInput) searchInput.value = '';
  if (clearBtn) clearBtn.classList.add('hidden');
  updateCategoryButtonStyles();
  fetchEvents();
}

/**
 * Fetch events from GET /api/events with category and search query params
 */
async function fetchEvents() {
  const loadingState = document.getElementById('loading-state');
  const errorState = document.getElementById('error-state');
  const emptyState = document.getElementById('empty-state');
  const grid = document.getElementById('events-grid');
  const countText = document.getElementById('events-count-text');

  // UI state transitions
  loadingState.classList.remove('hidden');
  errorState.classList.add('hidden');
  emptyState.classList.add('hidden');
  grid.innerHTML = '';
  countText.textContent = 'Updating...';

  try {
    const params = new URLSearchParams();
    if (currentCategory && currentCategory !== 'All') {
      params.append('category', currentCategory);
    }
    if (currentSearch && currentSearch.trim() !== '') {
      params.append('search', currentSearch.trim());
    }

    const url = `/api/events${params.toString() ? '?' + params.toString() : ''}`;
    const res = await fetch(url);

    if (!res.ok) {
      throw new Error(`Server returned ${res.status}: ${res.statusText}`);
    }

    const events = await res.json();
    loadingState.classList.add('hidden');

    if (!events || events.length === 0) {
      emptyState.classList.remove('hidden');
      countText.textContent = '0 events';
      return;
    }

    countText.textContent = `Showing ${events.length} event${events.length === 1 ? '' : 's'}`;
    renderEventCards(events);
  } catch (err) {
    console.error('Error fetching events:', err);
    loadingState.classList.add('hidden');
    errorState.classList.remove('hidden');
    const msg = document.getElementById('error-message-text');
    if (msg) msg.textContent = `Error connecting to API: ${err.message}`;
    countText.textContent = 'Failed to load';
  }
}

/**
 * Render array of events as cards into #events-grid
 */
function renderEventCards(events) {
  const grid = document.getElementById('events-grid');
  grid.innerHTML = '';

  events.forEach((ev, index) => {
    const isFull = ev.available_seats <= 0;
    const badgeColor = CATEGORY_COLORS[ev.category] || CATEGORY_COLORS.Default;
    const feeDisplay = ev.fee === 0 ? 'Free' : `₹${ev.fee}`;

    const card = document.createElement('div');
    card.className =
      'bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-200 flex flex-col animate-fade-in group';
    card.style.animationDelay = `${index * 40}ms`;

    card.innerHTML = `
      <div class="p-6 flex-1 flex flex-col">
        <!-- Card Header -->
        <div class="flex items-start justify-between gap-3 mb-3">
          <span class="text-3xl p-2.5 bg-slate-50 border border-slate-100 rounded-xl group-hover:scale-110 transition-transform">
            ${ev.icon || '⚡'}
          </span>
          <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${badgeColor}">
            ${ev.category}
          </span>
        </div>

        <!-- Title -->
        <h3 class="text-lg font-bold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-1 mb-2">
          ${escapeHtml(ev.title)}
        </h3>

        <!-- Description snippet -->
        <p class="text-xs text-slate-500 line-clamp-2 mb-4 leading-relaxed flex-1">
          ${escapeHtml(ev.description || 'No description provided.')}
        </p>

        <!-- Metadata Info -->
        <div class="space-y-2 py-3 border-t border-b border-slate-100 text-xs text-slate-600">
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-slate-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <span class="font-medium">${formatDate(ev.event_date)} • ${ev.event_time}</span>
          </div>
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-slate-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span class="truncate">${escapeHtml(ev.venue)}</span>
          </div>
        </div>

        <!-- Seats and Fee Bar -->
        <div class="flex items-center justify-between pt-3 text-xs">
          <div>
            ${
              isFull
                ? '<span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-black bg-rose-100 text-rose-700 uppercase tracking-wider">FULL</span>'
                : `<span class="font-bold text-emerald-600">${ev.available_seats}</span> <span class="text-slate-400">/ ${ev.capacity} seats left</span>`
            }
          </div>
          <div class="font-bold text-slate-800">
            ${feeDisplay}
          </div>
        </div>
      </div>

      <!-- Action Button -->
      <div class="p-4 bg-slate-50/60 border-t border-slate-100">
        <button
          onclick="openEventModal(${ev.id})"
          class="w-full py-2.5 px-4 rounded-xl text-xs font-bold transition-all flex items-center justify-center space-x-1 shadow-sm active:scale-95 ${
            isFull
              ? 'bg-slate-200 text-slate-700 hover:bg-slate-300'
              : 'bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow-indigo-500/25'
          }"
        >
          <span>${isFull ? 'View Details (Full)' : 'View Details & Register'}</span>
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>
    `;

    grid.appendChild(card);
  });
}

/**
 * Open Event Details & Registration Modal (Step 7)
 */
async function openEventModal(eventId) {
  try {
    const res = await fetch(`/api/events/${eventId}`);
    if (!res.ok) {
      alert(`Could not load event details: ${res.statusText}`);
      return;
    }
    const event = await res.json();
    currentModalEvent = event;

    // Populate modal fields
    document.getElementById('modal-event-icon').textContent = event.icon || '⚡';
    document.getElementById('modal-event-title').textContent = event.title;
    document.getElementById('modal-event-category').textContent = event.category;
    document.getElementById('modal-event-datetime').textContent = `${formatDate(event.event_date)} • ${event.event_time}`;
    document.getElementById('modal-event-venue').textContent = event.venue;
    document.getElementById('modal-event-fee').textContent = event.fee === 0 ? 'Free' : `₹${event.fee}`;
    document.getElementById('modal-event-description').textContent = event.description || 'No additional details provided.';
    document.getElementById('form-event-id').value = event.id;

    const seatsEl = document.getElementById('modal-event-seats');
    const isFull = event.available_seats <= 0;
    if (isFull) {
      seatsEl.innerHTML = '<span class="text-rose-600 font-black">FULL (0 seats left)</span>';
    } else {
      seatsEl.innerHTML = `<span class="text-emerald-600 font-bold">${event.available_seats}</span> / ${event.capacity} seats left`;
    }

    // Reset form fields and alerts
    const form = document.getElementById('registration-form');
    form.reset();
    resetValidationErrors();
    hideFormAlert();

    const submitBtn = document.getElementById('submit-reg-btn');
    const btnText = document.getElementById('btn-text');
    if (isFull) {
      submitBtn.disabled = true;
      btnText.textContent = 'Event is Full (Registrations Closed)';
      showFormAlert('warning', 'This event has reached full capacity. No further registrations can be accepted.');
    } else {
      submitBtn.disabled = false;
      btnText.textContent = 'Confirm Registration';
    }

    // Show backdrop
    const backdrop = document.getElementById('modal-backdrop');
    backdrop.classList.remove('hidden');
    document.body.classList.add('overflow-hidden');
  } catch (err) {
    console.error('Failed to open event modal:', err);
    alert('Failed to load event details. Please try again.');
  }
}

function closeModal() {
  const backdrop = document.getElementById('modal-backdrop');
  backdrop.classList.add('hidden');
  document.body.classList.remove('overflow-hidden');
  currentModalEvent = null;
}

/**
 * Handle student registration submission with client validation & API call
 */
async function handleRegistrationSubmit(e) {
  e.preventDefault();
  resetValidationErrors();
  hideFormAlert();

  const eventId = document.getElementById('form-event-id').value;
  const nameInput = document.getElementById('reg-name');
  const rollInput = document.getElementById('reg-roll');
  const emailInput = document.getElementById('reg-email');
  const phoneInput = document.getElementById('reg-phone');

  const studentName = nameInput.value.trim();
  const rollNumber = rollInput.value.trim();
  const email = emailInput.value.trim();
  const phone = phoneInput.value.trim();

  // Client-side validations
  let hasError = false;

  if (!studentName) {
    document.getElementById('err-name').classList.remove('hidden');
    nameInput.classList.add('border-rose-500');
    hasError = true;
  }

  if (!rollNumber) {
    document.getElementById('err-roll').classList.remove('hidden');
    rollInput.classList.add('border-rose-500');
    hasError = true;
  }

  if (!email || !email.includes('@')) {
    document.getElementById('err-email').classList.remove('hidden');
    emailInput.classList.add('border-rose-500');
    hasError = true;
  }

  if (hasError) return;

  // Submit to POST /api/events/{event_id}/register
  const submitBtn = document.getElementById('submit-reg-btn');
  const spinner = document.getElementById('btn-spinner');
  const btnText = document.getElementById('btn-text');

  submitBtn.disabled = true;
  spinner.classList.remove('hidden');
  btnText.textContent = 'Registering...';

  try {
    const payload = {
      student_name: studentName,
      roll_number: rollNumber,
      email: email,
      phone: phone || null
    };

    const res = await fetch(`/api/events/${eventId}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    const data = await res.json();

    if (res.status === 201) {
      // Success!
      showFormAlert(
        'success',
        `🎉 Successfully registered for ${currentModalEvent ? currentModalEvent.title : 'the event'}! Your registration ID is #${data.id}.`
      );
      btnText.textContent = 'Registered!';

      // Re-fetch events list to update seats in background
      fetchEvents();

      // Close modal automatically after 1.8 seconds
      setTimeout(() => {
        closeModal();
      }, 1800);
    } else {
      // Server returned 400, 409, or 404
      const serverMsg = data.detail || 'Registration failed. Please check your inputs.';
      showFormAlert('error', serverMsg);
      submitBtn.disabled = false;
      btnText.textContent = 'Try Again';
    }
  } catch (err) {
    console.error('Registration network error:', err);
    showFormAlert('error', 'Network error: could not connect to server.');
    submitBtn.disabled = false;
    btnText.textContent = 'Try Again';
  } finally {
    spinner.classList.add('hidden');
  }
}

function showFormAlert(type, message) {
  const alertBox = document.getElementById('form-alert');
  alertBox.classList.remove('hidden', 'bg-emerald-50', 'text-emerald-800', 'border-emerald-200', 'bg-rose-50', 'text-rose-800', 'border-rose-200', 'bg-amber-50', 'text-amber-800', 'border-amber-200');

  if (type === 'success') {
    alertBox.className = 'p-3 rounded-xl text-xs sm:text-sm font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200';
  } else if (type === 'warning') {
    alertBox.className = 'p-3 rounded-xl text-xs sm:text-sm font-semibold bg-amber-50 text-amber-800 border border-amber-200';
  } else {
    alertBox.className = 'p-3 rounded-xl text-xs sm:text-sm font-semibold bg-rose-50 text-rose-800 border border-rose-200';
  }

  alertBox.textContent = message;
}

function hideFormAlert() {
  const alertBox = document.getElementById('form-alert');
  if (alertBox) alertBox.classList.add('hidden');
}

function resetValidationErrors() {
  ['err-name', 'err-roll', 'err-email'].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.classList.add('hidden');
  });
  ['reg-name', 'reg-roll', 'reg-email'].forEach((id) => {
    const input = document.getElementById(id);
    if (input) input.classList.remove('border-rose-500');
  });
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  try {
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const monthIdx = parseInt(parts[1], 10) - 1;
      return `${months[monthIdx]} ${parseInt(parts[2], 10)}`;
    }
  } catch (e) {
    // fallback
  }
  return dateStr;
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

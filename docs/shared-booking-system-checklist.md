# Shree Banquet Hall — Android App Feature Preservation Checklist

The current APK is the functional and UI reference. Backend migration must not remove existing user-facing options.

## Existing app experience to preserve
- [ ] Existing calendar design and KR branding.
- [ ] Month navigation and date selection.
- [ ] Past-date handling and status summary.
- [ ] Admin login using username admin980123 and password.
- [ ] No Google/Gmail sign-in.
- [ ] Status selector: Available / Pending / Booked.
- [ ] Client name field.
- [ ] Client phone field.
- [ ] Event type field.
- [ ] Private notes field.
- [ ] Save booking action.
- [ ] Clear booking action.
- [ ] Upcoming bookings section filtered to selected month.
- [ ] Admin-only display of client details.
- [ ] Public calendar view without private client details.
- [ ] Loading and error states.

## Shared backend
- [ ] Supabase is the source of truth.
- [ ] Same booking data as both websites.
- [ ] Realtime updates.
- [ ] Auth session persistence and secure sign-out.
- [ ] RLS protects private booking data.
- [ ] Fresh start is acceptable; old Firebase booking history need not be migrated.

## Rollout safety
- [ ] Keep Firebase implementation/config until Supabase APK is built and tested.
- [ ] Work only on feature branch.
- [ ] Do not merge, publish, or disable Firebase without explicit approval.

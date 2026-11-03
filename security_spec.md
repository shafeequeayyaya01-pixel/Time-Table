# Security Specification: WAFY & WAFIYYA Co-ordinated Academic Timetable & Audit Portal

## 1. Data Invariants
1. **Authentication & Ownership**: Every document in `/users/{userId}/timetable_slots/{slotId}`, `/users/{userId}/period_audits/{auditId}`, `/users/{userId}/semester_configs/{configId}`, and `/users/{userId}/faculty_members/{facultyId}` must belong to an authenticated user (`request.auth != null`), and both the path `{userId}` and the document field `userId` must match `request.auth.uid`.
2. **Immutable Metadata**: `userId` and `createdAt` can never be mutated during an `update` operation (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
3. **Strict Key Allowlisting**: Every `create` must supply all required keys (`hasAll`) and only allowed keys (`hasOnly`). Every `update` must validate the post-write document with `isValid[Entity](incoming())` and restrict changed keys with `affectedKeys().hasOnly(...)`.
4. **Bounded Strings & Enums**: Every string field enforces strict `.size()` bounds, and enum fields (`campus`, `dayOfWeek`, `slotType`, `status`, `primaryCampus`, `preferredCluster`) are restricted to exact valid values.
5. **Temporal Integrity**: `createdAt` and `updatedAt` must be Firestore `timestamp` instances and `<= request.time`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/Write**: Reading or writing any timetable slot without `request.auth`.
2. **Cross-User Read/Write**: Authenticated user `alice_123` attempting to read or write `/users/bob_456/timetable_slots/slot_1`.
3. **Identity Spoofing on Create**: User `alice_123` writing to `/users/alice_123/timetable_slots/slot_1` with payload `userId: "bob_456"`.
4. **Shadow/Ghost Field Injection on Create**: Supplying an undeclared field `isAdminOverride: true` in `timetable_slots`.
5. **Shadow/Ghost Field Injection on Update**: Updating a `period_audits` document with an extra field `hacked: true`.
6. **Immutable `createdAt` Tampering**: Attempting to modify `createdAt` during an `update` on `timetable_slots`.
7. **Immutable `userId` Transfer**: Attempting to change `userId` during an `update` on `period_audits`.
8. **Invalid Enum Injection**: Creating a `TimetableSlot` with `campus: "INVALID_CAMPUS"` or `dayOfWeek: "SUN"`.
9. **Out-of-Range Period Number**: Creating a `TimetableSlot` with `periodNumber: 99` or `0` (must be `1..7`).
10. **Oversized String Denial-of-Wallet**: Sending a 5,000-character string in `missReason` or `subjectName`.
11. **Future Timestamp Spoofing**: Writing a `createdAt` timestamp set in the future (`> request.time`).
12. **Path ID Poisoning**: Using an invalid document ID exceeding 128 chars or containing special characters.

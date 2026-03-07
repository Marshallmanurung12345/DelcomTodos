# Todo Filtering Fix Plan

## Information Gathered
- **TodosScreen.kt**: Has filter UI with status chips (Semua/Selesai/Belum) and priority chips (All/High/Medium/Low)
- **TodoViewModel.kt**: Originally used API parameters for filtering, but backend may not support it properly
- **TodoApiService.kt**: Uses `@Query("is_done")` and `@Query("priority")` parameters
- **Issue**: Backend filtering not working, need client-side filtering

## Plan - COMPLETED
1. **Modified TodoViewModel.kt**: Added client-side filtering logic
   - Added `currentStatusFilter` and `currentPriorityFilter` to UIStateTodo
   - Added `applyClientSideFilter()` method to filter todos locally
   - Added `resetFilters()` method to reset filters

2. **Modified TodosScreen.kt**: Updated filter handling
   - Changed `fetchTodos()` to not pass filter parameters to API (fetch all data)
   - Added `applyFilters()` function that calls ViewModel's client-side filter
   - Added LaunchedEffect to automatically apply filters when filter state or data changes

3. **Modified TodosEditScreen.kt**: Added todo list refresh after update
   - Added `getAllTodos()` call after successful todo update to refresh the list
   - This ensures updated todo status is immediately reflected in the filter

## Files Edited
1. `app/src/main/java/org/delcom/pam_p5_ifs23021/ui/viewmodels/TodoViewModel.kt` ✓
2. `app/src/main/java/org/delcom/pam_p5_ifs23021/ui/screens/todos/TodosScreen.kt` ✓
3. `app/src/main/java/org/delcom/pam_p5_ifs23021/ui/screens/todos/TodosEditScreen.kt` ✓

## Testing
- Build the project to verify no compilation errors
- Test the filtering functionality:
  1. Create a new todo
  2. Change status to "Selesai" via edit screen
  3. Verify it appears in "Selesai" filter
  4. Verify it does NOT appear in "Belum" filter
  5. Test priority filtering works correctly


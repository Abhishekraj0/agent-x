# Context Management

Context management keeps interaction boundaries structured and respects token budgets.

## SlidingWindowContextManager
Tracks the last `N` messages, discarding older history to protect model attention and input costs.

## DefaultTokenBudgetManager
Tracks consumed input and output tokens using atomic properties to prevent unbounded cost overruns.

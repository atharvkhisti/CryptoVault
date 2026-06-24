# Frontend Architecture Specification - CryptoVault (Fully Responsive Redesign)

This document details the visual design system, brand guidelines, layout paradigms, component architecture, and API integration paths implemented in the **CryptoVault** frontend application.

---

## 1. Design Philosophy & Responsive Strategy

The CryptoVault frontend is built to deliver a consumer-first, high-trust, premium fintech interface modeled after the visual languages of **Phantom Wallet**, **Coinbase Wallet**, **Linear**, and **Stripe**. It focuses on mobile-inspired aesthetics adapted fluidly for larger viewports.

### Responsive Strategy
The application employs an **Adaptive Layout System** to maximize readability, accessibility, and visual polish across three device profiles:

1. **Mobile (< 768px)**:
   - Constrains layout to a linear vertical stack.
   - Utilizes a floating bottom navigation tab bar and a minimal top header.
   - Actions and disclosures reside in overlays and modal sheets, optimizing single-thumb usability.
2. **Tablet (768px - 1024px)**:
   - Transitions to a two-column grid.
   - Employs a sticky left sidebar navigation panel (`w-64`) to maximize active viewport space.
   - Pages like holdings and activity feeds adapt to grid splits.
3. **Desktop (> 1024px)**:
   - Uses fluid multi-column adaptive grids (`max-w-7xl` container width) to avoid stretched UI layouts.
   - Elements are distributed by priority (e.g., Home page splits balances and quick actions into columns 1 & 2, while placing timeline feeds and security alerts in column 3).
   - Sidebar navigation is fully expanded, ensuring access indicators and status pills are visible without browser zoom adjustments.

---

## 2. Design System & Theme Tokens

The visual style leverages glassmorphic transparency, soft drop shadows, and high contrast typography.

```
┌─────────────────────────────────────────────────────────────┐
│                       Theme Variables                       │
├─────────────────┬───────────────────────────────────────────┤
│ Background      │ #09090B (deep zinc-950 canvas)            │
│ Card            │ #18181B (zinc-900 glassmorphic card)       │
│ Primary         │ #8B5CF6 (violet-500 brand color)         │
│ Secondary       │ #6366F1 (indigo-500 hover states)         │
│ Accent          │ #14F195 (Solana neon green for success)   │
│ Text            │ #FAFAFA (zinc-50 high contrast text)      │
│ Muted           │ #A1A1AA (zinc-400 subheadings)           │
│ Radius          │ 24px+ (soft rounded profiles)             │
└─────────────────┴───────────────────────────────────────────┘
```

* **Rounded Corners**: A core variable of `24px` (`rounded-[24px]` / `rounded-[28px]`) is enforced on all main panels, buttons, inputs, and modals to maintain consistency with Phantom's visuals.
* **Glassmorphism**: Elements styled with `.glass-card` classes utilize `backdrop-filter: blur(16px)` combined with low-opacity borders (`border-zinc-800/80`) to enable radial background glows to diffuse smoothly.
* **Solana Green (`#14F195`)**: Selected as the core Accent color. Used exclusively for positive growth percentages, transaction completions, active connection indicators, and successful compliance statuses.

---

## 3. Brand & Logo Guidelines

CryptoVault features a premium, minimalist brand identity centered around a **single continuous line geometric snake symbol**.

```
          / \     <- Coiled head (Accented focus dot at #14F195)
         /   \
        /     \   <- Continuous line loop (#8B5CF6 to #6366F1 gradient)
       \       /
        \_____/
```

### Geometric Snake Representation
* **Concept**: A continuous vector line coiling from the bottom right tail, flowing through body waves, and terminating in the head at the top left with a glowing accent point. It represents fluid security, continuous protection, and digital ledger tracking.
* **Implementation** ([Logo.tsx](file:///d:/CryptoVault/frontend/web-app/src/components/ui/Logo.tsx)):
  - Built as a scalable SVG vector node rather than a bitmap file.
  - Automatically handles color variation states: `color` (uses CSS linear gradients from primary to secondary), `white` (`#FAFAFA`), and `black` (`#09090B`).
* **Visual Variants**:
  - `Logo`: Standard icon wrapper.
  - `FaviconLogo`: Compressed `16px` icon suitable for browser tabs.
  - `AppIconLogo`: Centered snake inside a `zinc-900` card frame with soft shadows.
  - `HeaderLogo`: Logo coupled with the bold wordmark "CryptoVault".

---

## 4. Component & Layout Architecture

### Core Layouts
* **`WalletLayout.tsx`**: Adapts dynamically:
  - On mobile, it displays a top header and a floating bottom navigation bar.
  - On tablet and desktop, it shifts to a sticky left sidebar nav, expanding the main workspace to `max-w-7xl` with responsive column grids.
* **`ProtectedRoute.tsx`**: Protects routing nodes and prevents access without a JWT token. Displays a skeleton containing structural page cards when verifying session tokens.
* **`ErrorBoundary.tsx`**: Captures runtime crashes and renders clean recovery fallback components.

### Page View Grids
* **Home Screen** ([Dashboard.tsx](file:///d:/CryptoVault/frontend/web-app/src/pages/Dashboard.tsx)):
  - Desktop view utilizes a `grid-cols-3` layout. Columns 1 & 2 contain the large total portfolio hero value, quick transaction actions (Send/Receive/Deposit/Withdraw), and the active token balance listings. Column 3 contains compliance alerts, unread messages, and chronological recent activity feeds.
* **Assets Page** ([Wallets.tsx](file:///d:/CryptoVault/frontend/web-app/src/pages/Wallets.tsx)):
  - Desktop view sets the holdings list inside a `grid-cols-2` layout on the left, while placing the Recharts donut share composition chart card on the right.
* **Transfer Page** ([Transactions.tsx](file:///d:/CryptoVault/frontend/web-app/src/pages/Transactions.tsx)):
  - Constrains the single transfer card and confirmation successor state to `max-w-md` centered on the desktop page.
* **Activity Page** ([Activity.tsx](file:///d:/CryptoVault/frontend/web-app/src/pages/Activity.tsx)):
  - Groups activities by **Today**, **Yesterday**, and **This Week**. Provides an inline horizontal selector bar to filter timeline elements dynamically.
* **Profile Page** ([Settings.tsx](file:///d:/CryptoVault/frontend/web-app/src/pages/Settings.tsx)):
  - Renders credentials and notification checkboxes in a left column, and identity document upload forms plus verification simulations in a two-thirds width right column.

---

## 5. API Ingress & State Synchronization

The application enforces **stateless Authorization token synchronization** through the API Gateway on port `8080`.

```
React 19 Client SPA
       │  (Requests include Bearer JWT in Authorization header)
       ▼
  API Gateway (Port 8080)
 ┌─────┴──────────────────────────┬──────────────────────────┐
 ▼ (X-USER-ID: UUID injected)      ▼ (X-USER-ID)              ▼ (X-USER-ID)
Auth Service (8083)        Wallet Service (8081)      Kyc Service (8087)
```

### Axios Client configuration (`services/axios.ts`)
- Automatically reads the access token from `localStorage` and injects it into headers.
- Intercepts `401 Unauthorized` responses and logs out the user, redirecting them to `/login`.

### Query Invalidation Paradigm
Using **TanStack Query** (React Query), transaction operations automatically trigger updates:
- Deposits, withdrawals, and transfers execute, then call `queryClient.invalidateQueries({ queryKey: ['wallets'] })` and `['transactions']`.
- Balance hero displays, token lists, and activity logs update instantly without full-page reloads.

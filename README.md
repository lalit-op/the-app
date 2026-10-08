# One Read - All Document Reader & PDF Suite

**One Read** is an all-in-one Android document reader and PDF suite built with modern Kotlin and Jetpack Compose (Material Design 3). It enables users to read, organize, convert, and manage documents offline with high privacy.

## Features

- **Multi-Format Document Viewing**:
  - **PDF Reader**: Built-in hardware-accelerated PDF rendering with continuous vertical scrolling and page-by-page swipe modes, pinch-to-zoom, pan, jump-to-page dialog, and night reading / invert color mode.
  - **Text & Code Viewer**: Reads `.txt`, `.json`, `.xml`, `.csv`, `.java`, `.kt`, `.cpp`, `.html`, `.md`, `.rtf`, and `.log` with live text search & occurrence highlighting, line numbers toggle, font scaling (A- / A+), word wrap toggle, and 3 reading color themes (Day, Eye Care / Sepia, Night).
  - **Spreadsheet & Data Viewer**: Reads CSV and table data with instant parsing.
  - **Image Viewer**: High-resolution image preview with zoom/pan and one-tap "Convert to PDF" action.

- **PDF & Document Utilities**:
  - **Image to PDF Converter**: Choose multiple photos from device storage using the zero-permission Android Photo Picker, adjust page orientation (Portrait / Landscape), reorder or delete pages, and generate a new PDF.
  - **Merge PDF**: Combine 2 or more PDF documents into a single document.
  - **Split PDF**: Select any PDF and extract specified individual pages or ranges into a separate PDF.
  - **Print Service**: Direct wireless or export-to-PDF printing via Android's native `PrintManager`.

- **File Management & Organization**:
  - **Category Tabs**: Filter by All, PDF, Word, Excel, PowerPoint, Text, and Images.
  - **Search & Sort**: Real-time title search and multi-criteria sorting (by Date, Name, or Size).
  - **Favorites**: Star and quickly access important documents.
  - **Recent History**: Automatically tracks opened documents and bookmarks the last read page.
  - **Safe Recycle Bin**: Move documents to trash and restore them anytime or empty the bin permanently.
  - **File Operations**: In-place rename, share via Android Intent, and full file metadata inspector.

- **Design & Performance**:
  - Modern Material Design 3 interface with dynamic color support and dark/light mode switching.
  - Custom adaptive Material launcher icon.
  - 100% offline-first architecture with local SQLite persistence and zero tracking.

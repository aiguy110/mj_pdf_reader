* V1.4.4


* V1.4.3+
  * Big increase in performance, especially for big files.
  * Removed the most common causes of crashing.
  * Decreased ram usage significantly.
  * Added 'Go To Page' option.
  * Added an option (seekbar) to adjust brightness in the Full Screen Mode
  * Search is now available for files of any
  * Better and more consistent theme across the app.
  * Changed App Bar style. (font, color, icons, title max lines)
  * Clicking on the title will show a message containing the full name of the pdf.
  * Changed scroll handler style.
  * Moved 'Print File' to the main menu, and put 'About' to the additional options.
  * Relabeled 'Additional Options' as 'More'
  * Disabled Text Mode since it's not usable yet and crashes a lot.
  * Hid page scroll handle if the pdf consists of only one page.
  * Improved Copy Page's Text functionality and UI.


* V1.4.2
    * Add an option to turn the page using volume buttons.
    * Add a button to disable copy page text pop up on long press.
    * Fix NumberFormatException when local use comma for decimal point.
  
* V1.4.1
    * A workaround to prevent app from crashing when opening huge files.
  
* V1.4.0
    * Updated the core libraries and fixed the security issue.
    * Added Search functionality. (experimental) ([see Text Mode and Search](https://gitlab.com/mudlej_android/mj_pdf_reader#text-mode-and-search))
    * Added Text mode to view PDFs like E-readers. (experimental) ([see Text Mode and Search](https://gitlab.com/mudlej_android/mj_pdf_reader#text-mode-and-search))
    * Added the ability to copy text from the PDF via a dialog.
    * Reorganized action bar's options and added Additional Options.


## What is different from PDF Viewer Plus
- [x] Great refactoring of the code. (still needs more)
- [x] Converted the code to Kotlin. (except for two files)
- [x] Removed many security vulnerabilities. ([see Forking PdfiumAndroid](https://gitlab.com/mudlej_android/mj_pdf_reader#forking-pdfiumandroid))
- [x] Extracted almost all the resources from the logic files.
- [x] Support for the new versions of Android.
- [x] Forked and changed android-pdf-viewer library to achieve some of the below modifications.
- [x] Forked [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid) to update its core libraries. ([see Forking PdfiumAndroid](https://gitlab.com/mudlej_android/mj_pdf_reader#forking-pdfiumandroid))
- [x] Removed [WhatsNew](https://github.com/TonnyL/WhatsNew) dependency.
- [x] Removed [Cyanea](https://github.com/jaredrummler/Cyanea) dependency.
- [x] Removed deprecated PreferenceActivity and replaced it with AndroidX Preference library and rewrote all the files.
- [x] Changed the license to GPLv3
- [x] Remembers the last opened page of each PDF. (credits to @Derekelkins)
- [x] True full screen mode
  - While in the full screen mode, there are no buttons nor bars that will pop up very annoyingly when scrolling, instead tapping the screen will show / hide three elements to control the viewer:
    - A simple button to get out of the full screen. (top-left corner)
    - A simple button to rotate the screen and lock the rotation while in the full screen mode. (top-left corner)
    - A page scroll handle.
- [x] Adding a new library that will work in the background to extract PDFs text.
- [x] Adding a Text Mode that tries to view the PDF like epub files. ([see Text Mode and Search](https://gitlab.com/mudlej_android/mj_pdf_reader#text_mode_and_search))
- [x] Adding a search functionality. ([see Text Mode and Search](https://gitlab.com/mudlej_android/mj_pdf_reader#text_mode_and_search))
- [x] Adding the ability to copy text from the PDF via a dialog that will show up by a long press on the page.
- [x] Fixed the (very annoying) issue with full screen mode being lost when the screen gets rotated.
- [x] Added TextView that will be visible only when scrolling, so you can see the page number while scrolling.
- [x] Added the functionality to restore the zoom level when rotating the device via a Snackbar prompt.
- [x] Removed the Bottom Bar & placed its options in the action menu.
- [x] Placed the toggle full screen button in the top bar.
- [x] Changed the title format in the top bar to show the pages count first, and removed the '.pdf' form it.
- [x] Changed the Default Theme to the Material theme.
- [x] App's light / dark theme follows phone's theme. (You've to enable this option, disabled by default due to https://github.com/barteksc/AndroidPdfViewer/issues/914)
- [x] A shortcut to switch the theme for the PDF in the action menu.
- [x] Decreased the size of the app from 20 MB to 7 MB. (MJ PDF Reader v1.4)
- [x] See the above Release Notes for further changes and improvements.
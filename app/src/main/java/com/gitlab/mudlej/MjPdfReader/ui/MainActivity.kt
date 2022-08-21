/*
 *   MJ PDF Reader
 *   Copyright (C) 2022 Mudlej
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  --------------------------
 *  This code was previously licensed under
 *
 *  MIT License
 *
 *  Copyright (c) 2018 Gokul Swaminathan
 *  Copyright (c) 2022 Mudlej
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.gitlab.mudlej.MjPdfReader.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.print.PrintManager
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.PreferenceManager
import com.github.barteksc.pdfviewer.PDFView.Configurator
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.scroll.ScrollHandle
import com.github.barteksc.pdfviewer.util.Constants
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.gitlab.mudlej.MjPdfReader.PdfDocumentAdapter
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.AppDatabase
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.data.Preferences
import com.gitlab.mudlej.MjPdfReader.data.SavedLocation
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityMainBinding
import com.gitlab.mudlej.MjPdfReader.databinding.PasswordDialogBinding
import com.gitlab.mudlej.MjPdfReader.util.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.shockwave.pdfium.PdfPasswordException
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    enum class AdditionalOptions{ METADATA, APP_SETTINGS, PRINT_FILE, ADVANCED_CONFIG }

    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val tappingHandler = Handler(Looper.getMainLooper())

    private lateinit var pref: Preferences
    private lateinit var database: AppDatabase
    private val pdf = PDF()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "-----------onCreate: ${pdf.name} ")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // To avoid FileUriExposedException, (https://stackoverflow.com/questions/38200282/)
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())

        // init
        pref = Preferences(PreferenceManager.getDefaultSharedPreferences(this))
        database = AppDatabase.getInstance(applicationContext)

        Constants.THUMBNAIL_RATIO = pref.getThumbnailRation()
        Constants.PART_SIZE = pref.getPartSize()


        // Show Into Activity and Features Dialog on the first install
        onFirstInstall()

        // Create PDF by restore it in case of activity restart OR open filer picker
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)
        }
        else {
            pdf.uri = intent.data
            if (pdf.uri == null) pickFile()
        }

        displayFromUri(pdf.uri)
        setButtonsFunctionalities()
        showAppFeaturesDialogOnFirstRun()
    }

    private fun onFirstInstall() {
        val isFirstRun = pref.getFirstInstall()
        if (isFirstRun) {
            startActivity(Intent(this, MainIntroActivity::class.java))
            pref.setFirstInstall(false)
            pref.setShowFeaturesDialog(true)
        }
    }

    private fun openSelectedDocument(selectedDocumentUri: Uri?) {
        if (selectedDocumentUri == null) return

        if (pdf.uri == null || selectedDocumentUri == pdf.uri) {
            pdf.uri = selectedDocumentUri
            displayFromUri(pdf.uri)
        } else {
            val intent = Intent(this, javaClass)
            intent.data = selectedDocumentUri
            startActivity(intent)
        }
    }

    private fun pickFile() {
        try {
            documentPickerLauncher.launch(arrayOf(PDF.FILE_TYPE))
        } catch (e: ActivityNotFoundException) {
            // alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun displayFromUri(uri: Uri?) {
        if (uri == null) return

        pdf.name = getFileName(this, uri)
        title = pdf.name
        setTaskDescription(ActivityManager.TaskDescription(pdf.name))
        val scheme = uri.scheme
        if (scheme != null && scheme.contains("http")) {
            downloadOrShowDownloadedFile(uri)
        } else {
            initPdfViewAndLoad(binding.pdfView.fromUri(pdf.uri))
        }
    }

    private fun initPdfViewAndLoad(viewConfigurator: Configurator) {
        // start extracting text in the background
        extractPdfText()

        // attempt to find a saved location for the pdf else assign zero
        if (pdf.pageNumber == 0) {
            executor.execute {
                // off UI thread
                pdf.fileHash = computeHash(this, pdf)
                pdf.pageNumber = database.savedLocationDao().findSavedPage(pdf.fileHash) ?: 0

                // back to UI thread
                handler.post {
                    initPdfViewAndLoad(viewConfigurator, pdf.pageNumber)
                }
            }
        }
        else initPdfViewAndLoad(viewConfigurator, pdf.pageNumber)
    }

    private fun initPdfViewAndLoad(viewConfigurator: Configurator, pageNumber: Int) {
        configureTheme()

        val pdfView = binding.pdfView
        pdfView.useBestQuality(pref.getHighQuality())
        pdfView.minZoom = Preferences.minZoomDefault
        pdfView.midZoom = Preferences.midZoomDefault
        pdfView.maxZoom = pref.getMaxZoom()
        pdfView.zoomTo(pdf.zoom)

        viewConfigurator   // creates a Configurator
            .defaultPage(pageNumber)
            .onPageChange { page: Int, pageCount: Int -> setCurrentPage(page, pageCount)}
            .enableAnnotationRendering(Preferences.annotationRenderingDefault)
            .enableAntialiasing(pref.getAntiAliasing())
            .onTap { toggleScrollAndButtonsVisibility() }
            .onLongPress { showPageTextDialog() }
            .scrollHandle(DefaultScrollHandle(this))
            .spacing(Preferences.spacingDefault)
            .onError { exception: Throwable -> handleFileOpeningError(exception) }
            .onPageError { page: Int, error: Throwable -> reportLoadPageError(page, error) }
            .pageFitPolicy(FitPolicy.WIDTH)
            .password(pdf.password)
            .swipeHorizontal(pref.getHorizontalScroll())
            .autoSpacing(pref.getHorizontalScroll())
            .pageSnap(pref.getPageSnap())
            .pageFling(pref.getPageFling())
            .nightMode(pref.getPdfDarkTheme())
            .load()

        // Show the page scroll handler for 3 seconds when the pdf is loaded then hide it.
        pdfView.performTap()
        tappingHandler.postDelayed({ hideButtons(pdfView.scrollHandle) },
            pref.getHideDelay().toLong())
    }

    private fun showPageTextDialog() {
        // TODO: create an option to disable this dialog
        // if (pref.getIsPageTextDialogDisabled) return

        // copy page's text or set an appropriate message
        val pageText = (pdf.pagesText[pdf.pageNumber + 1] ?: "")
            .ifEmpty { "Couldn't extract text from this page"}

        // create a custom view to make the text selectable
        val pageTextView = TextView(this)
        pageTextView.setPadding(30, 10, 30, 10)
        pageTextView.setTextIsSelectable(true)
        pageTextView.text = pageText
        pageTextView.textSize = 16f

        AlertDialog.Builder(this)
            .setView(pageTextView)
            .setTitle("Page #${pdf.pageNumber + 1} Selectable Text (experimental)")
            .setPositiveButton("Copy All") { dialog, _ ->
                // copy page's text to clipboard
                val clipboard: ClipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData = ClipData.newPlainText(
                    "Page #${pdf.pageNumber} Text", pageText
                )
                clipboard.setPrimaryClip(clip)

                // show message to user before closing
                Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun extractPdfText() {
        var document: PDDocument? = null
        val pdfStripper = PDFTextStripper()
        pdf.pagesText.clear()

        Executors.newSingleThreadExecutor().execute {
            // off UI thread
            try {
                document = PDDocument.load(contentResolver.openInputStream(pdf.uri as Uri))
                val pagesCount = document?.numberOfPages ?: 0

                for (i in 0..pagesCount) {      // <= intentional
                    pdfStripper.startPage = i
                    pdfStripper.endPage = i
                    pdf.pagesText[i] = pdfStripper.getText(document)
                }
                // back to UI thread
                handler.post {
                    Log.d(TAG, "extractPdfText: finished")
                    pdf.isExtractingTextFinished = true
                    invalidateOptionsMenu()     // update Text-Mode option in action bar
                }
            }
            catch (e: IOException) {
                Log.e("PdfBox", "Exception thrown while stripping text", e)
            }
            finally {
                try {
                    document?.close()
                }
                catch (e: IOException) {
                    Log.e("PdfBox", "Exception thrown while closing document", e)
                }
            }
        }

    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setButtonsFunctionalities() {
        binding.exitFullScreenButton.setOnClickListener {
            // set orientation to unspecified so that the screen rotation will be unlocked
            // this is because PORTRAIT / LANDSCAPE modes will lock the app in them
            toggleFullscreen(false)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            hideButtons(null)
        }
        binding.rotateScreenButton.setOnClickListener {
            requestedOrientation =
                if (pdf.isPortrait) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            pdf.togglePortrait()
        }
        binding.pickFile.setOnClickListener { pickFile() }
    }

    public override fun onResume() {
        Log.i(TAG, "-----------onResume: ${pdf.name} ")
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (pref.getScreenOn()) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // check if there is a pdf at first
//        if (pdf.uri == null) return

        if (pdf.uri != null) binding.pickFile.visibility = View.GONE

        // restore the full screen mode if was toggled On
        if (pdf.isFullScreenToggled) toggleFullscreen(true)

        // Prompt the user to restore the previous zoom if there is one saved other than the default
        // pdfZoom != binding.pdfView.getZoom())   // doesn't work for some peculiar reason
        if (pdf.zoom != 1f) {
            Snackbar.make(findViewById(R.id.main),
                getString(R.string.ask_restore_zoom), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.restore)) {
                    binding.pdfView.zoomWithAnimation(pdf.zoom)
                }
                .show()
        }
        fixButtonsColor()
    }

    override fun onPause() {
        Log.i(TAG, "-----------onPause: ${pdf.name} ")
        super.onPause()
    }

    override fun onDestroy() {
        Log.i(TAG, "-----------onDestroy: ${pdf.name} ")
        super.onDestroy()
    }

    private fun fixButtonsColor() {
        // changes buttons color
        val color = if (pref.getPdfDarkTheme()) R.color.bright else R.color.dark
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.exitFullScreenImage.drawable),
            ContextCompat.getColor(this, color)
        )
        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.rotateScreenImage.drawable),
            ContextCompat.getColor(this, color)
        )
    }

    private fun shareFile() {
        val uri = pdf.uri
        if (uri == null) {
            checkHasFile()  // only to show the message
            return
        }
        val sharingIntent: Intent = 
            if (uri.scheme != null && uri.scheme!!.startsWith("http"))
                plainTextShareIntent(getString(R.string.share), pdf.uri.toString())
            else 
                fileShareIntent(getString(R.string.share), pdf.name, uri)
        
        startActivity(sharingIntent)
    }

    private fun configureTheme() {
        // This should be moved to the onCreate or xml files
        window.statusBarColor = Color.parseColor("#1a1b1b")

        val pdfView = binding.pdfView

        // set background color behind pages
        if (!pref.getPdfDarkTheme()) pdfView.setBackgroundColor(Preferences.pdfDarkBackgroundColor) else pdfView.setBackgroundColor(
            Preferences.pdfLightBackgroundColor
        )
        if (pref.getAppFollowSystemTheme()) {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        } else {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun reportLoadPageError(page: Int, error: Throwable) {
        val message = resources.getString(R.string.cannot_load_page) + page + " " + error
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, message)
    }

    private fun hideButtons(handle: ScrollHandle?) {
        // stop any previous timer to hide them
        tappingHandler.removeCallbacksAndMessages(null)

        handle?.customHide()
        binding.exitFullScreenButton.visibility = View.INVISIBLE
        binding.rotateScreenButton.visibility = View.INVISIBLE
    }

    private fun toggleScrollAndButtonsVisibility(): Boolean {
        val handle = binding.pdfView.scrollHandle
        val exitButton = binding.exitFullScreenButton
        val rotateButton = binding.rotateScreenButton
        if (handle == null) {
            toggleButtonsVisibility()
            return true
        }

        // timer to hide them. This timer will be canceled in the else branch
        tappingHandler.removeCallbacksAndMessages(null)
        handle.cancelHideRunner()

        // try
//        f()

        // set a new timer to hide
        tappingHandler.postDelayed({
            exitButton.visibility = View.INVISIBLE
            rotateButton.visibility = View.INVISIBLE
            handle.customHide()
        }, pref.getHideDelay().toLong())
        if (!handle.customShown()) {
            handle.customShow()
            if (pdf.isFullScreenToggled) {
                exitButton.visibility = View.VISIBLE
                rotateButton.visibility = View.VISIBLE
            }
        } else if (exitButton.visibility == View.GONE && pdf.isFullScreenToggled) {
            exitButton.visibility = View.VISIBLE
            rotateButton.visibility = View.VISIBLE
        } else {
            hideButtons(handle)
        }
        return true
    }

    private fun toggleButtonsVisibility() {
        if (!pdf.isFullScreenToggled) return
        val exitButton = binding.exitFullScreenButton
        val rotateButton = binding.rotateScreenButton
        if (exitButton.visibility == View.VISIBLE) {
            exitButton.visibility = View.INVISIBLE
            rotateButton.visibility = View.INVISIBLE
        } else {
            exitButton.visibility = View.VISIBLE
            rotateButton.visibility = View.VISIBLE
        }
    }

    private fun handleFileOpeningError(exception: Throwable) {
        if (exception is PdfPasswordException) {
            if (pdf.password != null) {
                Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                pdf.password = null // prevent the toast if the user rotates the screen
            }
            askForPdfPassword()
        } else if (couldNotOpenFileDueToMissingPermission(exception)) {
            readFileErrorPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show()
            Log.e(TAG, getString(R.string.file_opening_error), exception)
        }
    }

    private fun couldNotOpenFileDueToMissingPermission(e: Throwable): Boolean {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) return false
        val exceptionMessage = e.message
        return e is FileNotFoundException && exceptionMessage != null
                && exceptionMessage.contains(getString(R.string.permission_denied))
    }

    private fun restartAppIfGranted(isPermissionGranted: Boolean) {
        if (isPermissionGranted) {
            // This is a quick and dirty way to make the system restart the current activity *and the current app process*.
            // This is needed because on Android 6 storage permission grants do not take effect until
            // the app process is restarted.
            exitProcess(0)
        } else {
            Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleFullscreen(fixFullScreen: Boolean) {
        val view: View = binding.pdfView
        if (!pdf.isFullScreenToggled || fixFullScreen) {
            supportActionBar?.hide()
            pdf.isFullScreenToggled = true
            view.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

            // hide the scroll handle
            if (!fixFullScreen) {
                val handle = binding.pdfView.scrollHandle
                handle?.customHide()
            }

            // show how to dialog
            if (pref.getShowFeaturesDialog()) showHowToExitFullscreenDialog(this, pref)
        } else {
            supportActionBar?.show()
            pdf.isFullScreenToggled = false
            view.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun downloadOrShowDownloadedFile(uri: Uri) {
        if (pdf.downloadedPdf == null) {
            pdf.downloadedPdf = lastCustomNonConfigurationInstance as ByteArray?
        }
        if (pdf.downloadedPdf != null) {
            initPdfViewAndLoad(binding.pdfView.fromBytes(pdf.downloadedPdf))
        } else {
            // we will get the pdf asynchronously with the DownloadPDFFile object
            binding.progressBar.visibility = View.VISIBLE
            val downloadPDFFile =
                DownloadPDFFile(this)
            downloadPDFFile.execute(uri.toString())
        }
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        return pdf.downloadedPdf
    }

    fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    fun saveToFileAndDisplay(pdfFileContent: ByteArray?) {
        pdf.downloadedPdf = pdfFileContent
        saveToDownloadFolderIfAllowed(pdfFileContent)
        initPdfViewAndLoad(binding.pdfView.fromBytes(pdfFileContent))
    }

    private fun saveToDownloadFolderIfAllowed(fileContent: ByteArray?) {
        if (canWriteToDownloadFolder(this)) {
            trySaveToDownloads(fileContent, false)
        } else {
            saveToDownloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun trySaveToDownloads(fileContent: ByteArray?, showSuccessMessage: Boolean) {
        try {
            val downloadDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            writeBytesToFile(downloadDirectory, pdf.name, fileContent)
            if (showSuccessMessage) {
                Toast.makeText(this, R.string.saved_to_download, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e(TAG, getString(R.string.save_to_download_failed), e)
            Toast.makeText(this, R.string.save_to_download_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDownloadedFileAfterPermissionRequest(isPermissionGranted: Boolean) {
        if (isPermissionGranted) {
            trySaveToDownloads(pdf.downloadedPdf, true)
        } else {
            Toast.makeText(this, R.string.save_to_download_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navToAppSettings() {
        settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
    }

    private fun setCurrentPage(pageNumber: Int, pageCount: Int) {
        pdf.pageNumber = pageNumber             // I think this may need to be incremented
        pdf.setPageCount(pageCount)
        title = pdf.getTitle()

        val hash = pdf.fileHash              // Don't want fileContentHash to change out from under us
        if (hash != null) executor.execute {    // off UI thread
            database.savedLocationDao().insert(SavedLocation(hash, pdf.pageNumber))
        }
    }

    private fun printFile() {
        if (checkHasFile()) {
            val mgr = getSystemService(Context.PRINT_SERVICE) as PrintManager
            mgr.print(pdf.name, PdfDocumentAdapter(this, pdf.uri), null)
        }
    }

    private fun askForPdfPassword() {
        val dialogBinding = PasswordDialogBinding.inflate(layoutInflater)
        showAskForPasswordDialog(this, pdf, dialogBinding, ::displayFromUri)
    }

    private fun showAppFeaturesDialogOnFirstRun() {
        if (pref.getShowFeaturesDialog()) {
            Handler(mainLooper).postDelayed({ showAppFeaturesDialog(this) }, 500)
            pref.setShowFeaturesDialog(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.fullscreen_option -> toggleFullscreen(false)
            R.id.switch_theme -> switchPdfTheme()
            R.id.open_file -> pickFile()
            R.id.bookmarks_list -> showBookmarksDialog(this, binding.pdfView)
            R.id.text_mode -> navToTextMode()
            R.id.share_file -> shareFile()
            R.id.additional_options -> showAdditionalOptions()
            R.id.action_about -> {
                startActivity(navIntent(this, AboutActivity::class.java))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (pdf.isExtractingTextFinished) {
            val textModeItem = menu.findItem(R.id.text_mode)
            textModeItem.title = getString(R.string.text_mode)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun navToTextMode() {
        if (!checkHasFile()) return

        if (!pdf.isExtractingTextFinished) {
            Toast.makeText(this,
                "The app is still extracting text from the PDF file", Toast.LENGTH_LONG).show()

            return
        }
        val intent = Intent(this, TextMode::class.java)
        // this can easily crash the app, the transaction is too big
        intent.putExtra(Preferences.pdfTextKey, Gson().toJson(pdf.pagesText))
        intent.putExtra(Preferences.pdfLengthKey, pdf.length)
        startActivity(intent)
    }

    private fun showAdditionalOptions() {
        // map an index to an option string
        val settingsMap = mapOf(
            AdditionalOptions.METADATA to getString(R.string.metadata),
            AdditionalOptions.APP_SETTINGS to getString(R.string.app_settings),
            AdditionalOptions.PRINT_FILE to getString(R.string.print_file),
            AdditionalOptions.ADVANCED_CONFIG to getString(R.string.advanced_config)
        )

        // create a dialog for additional options and set their functionalities
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings))
            .setItems(settingsMap.values.toTypedArray()) { dialog, which ->
                when (which) {
                    AdditionalOptions.METADATA.ordinal ->
                        if (checkHasFile()) showMetaDialog(this, binding.pdfView.documentMeta)
                    AdditionalOptions.APP_SETTINGS.ordinal -> navToAppSettings()
                    AdditionalOptions.PRINT_FILE.ordinal -> printFile()
                    AdditionalOptions.ADVANCED_CONFIG.ordinal -> showPartSizeDialog(this, pref)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun checkHasFile(): Boolean {
        if (!pdf.hasFile()) {
            Snackbar.make(binding.root, getString(R.string.no_pdf_in_app),
                Snackbar.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun switchPdfTheme() {
        if (checkHasFile()) {
            pref.setPdfDarkTheme(!pref.getPdfDarkTheme())
            recreate()
        }
    }

    private val documentPickerLauncher = registerForActivityResult(OpenDocument()) {
            selectedDocumentUri: Uri? -> openSelectedDocument(selectedDocumentUri)
    }

    private val saveToDownloadPermissionLauncher = registerForActivityResult(RequestPermission()) {
            isPermissionGranted: Boolean -> saveDownloadedFileAfterPermissionRequest(isPermissionGranted)
    }

    private val readFileErrorPermissionLauncher = registerForActivityResult(RequestPermission()) {
            isPermissionGranted: Boolean -> restartAppIfGranted(isPermissionGranted)
    }

    private val settingsLauncher = registerForActivityResult(StartActivityForResult()) {
        displayFromUri(pdf.uri)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(PDF.uriKey, pdf.uri)
        outState.putInt(PDF.pageNumberKey, pdf.pageNumber)
        outState.putString(PDF.passwordKey, pdf.password)
        outState.putBoolean(PDF.isFullScreenToggledKey, pdf.isFullScreenToggled)
        outState.putFloat(PDF.zoomKey, binding.pdfView.zoom)
        super.onSaveInstanceState(outState)
    }

    private fun restoreInstanceState(savedState: Bundle) {
        pdf.uri = savedState.getParcelable(PDF.uriKey)
        pdf.pageNumber = savedState.getInt(PDF.pageNumberKey)
        pdf.password = savedState.getString(PDF.passwordKey)
        pdf.isFullScreenToggled = savedState.getBoolean(PDF.isFullScreenToggledKey)
        pdf.zoom = savedState.getFloat(PDF.zoomKey)
    }
}

/*
    * pdf.pageNumber && pdf.length:
        will be set by PDFView::onPageChange() -> setCurrentPage()

    * pdf.password:
        will be set by PDFView::onError() -> handleFileOpeningError() -> askForPdfPassword()
 */
package com.gitlab.mudlej.MjPdfReader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.print.PrintManager;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.scroll.ScrollHandle;
import com.github.barteksc.pdfviewer.util.Constants;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.gitlab.mudlej.MjPdfReader.data.PDF;
import com.gitlab.mudlej.MjPdfReader.data.Preferences;
import com.google.android.material.snackbar.Snackbar;
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityMainBinding;
import com.gitlab.mudlej.MjPdfReader.databinding.PasswordDialogBinding;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfPasswordException;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.gitlab.mudlej.MjPdfReader.Utils.showAppFeaturesDialog;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding viewBinding;

    /* For performance reasons, we won't hash the entire PDF but only up to this many bytes. */

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler tappingHandler = new Handler();

    private PrintManager mgr;
    private AppDatabase appDb;
    private Preferences pref;

    private byte[] downloadedPdfFileContent;
    private final PDF pdf = new PDF();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        Constants.THUMBNAIL_RATIO = 1f;

        // Workaround for https://stackoverflow.com/questions/38200282/
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        pref = new Preferences(PreferenceManager.getDefaultSharedPreferences(this));
        mgr = (PrintManager) getSystemService(PRINT_SERVICE);
        appDb = AppDatabase.getInstance(getApplicationContext());

        onFirstInstall();
        onFirstUpdate();

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        } else {
            pdf.setUri(getIntent().getData());
            if (pdf.getUri() == null)
                pickFile();
        }
        displayFromUri(pdf.getUri());

        setButtonsFunctionalities();
        showAppFeaturesDialogOnFirstRun();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void setButtonsFunctionalities() {
        viewBinding.exitFullScreenButton.setOnClickListener(view -> {
            // set orientation to unspecified so that the screen rotation will be unlocked
            // this is because PORTRAIT / LANDSCAPE modes will lock the app in them
            toggleFullscreen(false);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            hideButtons(null);
        });

        viewBinding.rotateScreenButton.setOnClickListener(view -> {
            if (pdf.isPortrait())
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            else
                // exitFullScreenButton will put it in Unspecified
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            pdf.togglePortrait();
        });

        viewBinding.pickFile.setOnClickListener(view -> pickFile());
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (pref.getScreenOn())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // restore the full screen mode if was toggled On
        if (pdf.isFullScreenToggled()) toggleFullscreen(true);

        // Prompt the user to restore the previous zoom if there is one saved other than the default
        // pdfZoom != viewBinding.pdfView.getZoom())   // doesn't work for some peculiar reason
        if (pdf.getZoom() != 1) {
            Snackbar.make(findViewById(R.id.main),
                            getString(R.string.ask_restore_zoom), Snackbar.LENGTH_LONG
            )
                .setAction(getString(R.string.restore), view ->
                        viewBinding.pdfView.zoomWithAnimation(pdf.getZoom()))
                .show();
        }

        fixButtonsColor();

        // if there data in the pdf source variable (local path or url), hide the pickFile Button
        if (pdf.getUri() != null) viewBinding.pickFile.setVisibility(View.GONE);
    }

    private void fixButtonsColor() {
        // changes buttons color
        int color = pref.getPdfDarkTheme() ? R.color.bright : R.color.dark;
        DrawableCompat.setTint(
                DrawableCompat.wrap(viewBinding.exitFullScreenImage.getDrawable()),
                ContextCompat.getColor(this, color)
        );
        DrawableCompat.setTint(
                DrawableCompat.wrap(viewBinding.rotateScreenImage.getDrawable()),
                ContextCompat.getColor(this, color)
        );
    }

    private void onFirstInstall() {
        boolean isFirstRun = pref.getFirstInstall();

        if (isFirstRun) {
            startActivity(new Intent(this, MainIntroActivity.class));

            pref.setFirstInstall(false);
            pref.setShowFeaturesDialog(true);
        }
    }

    private void onFirstUpdate() {
        boolean isFirstRun = pref.getAppVersion();
        if (isFirstRun) pref.setAppVersion(false);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(PDF.uriKey, pdf.getUri());
        outState.putInt(PDF.pageNumberKey, pdf.getPageNumber());
        outState.putString(PDF.passwordKey, pdf.getPassword());
        outState.putBoolean(PDF.isFullScreenToggledKey, pdf.isFullScreenToggled());
        //outState.putFloat(PDF.zoomKey, viewBinding.pdfView.getZoom());
        outState.putFloat(PDF.zoomKey, viewBinding.pdfView.getZoom());
        super.onSaveInstanceState(outState);
    }

    private void restoreInstanceState(Bundle savedState) {
        pdf.setUri(savedState.getParcelable(PDF.uriKey));
        pdf.setPageNumber(savedState.getInt(PDF.pageNumberKey));
        pdf.setPassword(savedState.getString(PDF.passwordKey));
        pdf.setFullScreenToggled(savedState.getBoolean(PDF.isFullScreenToggledKey));
        pdf.setZoom(savedState.getFloat(PDF.zoomKey));
    }

    void shareFile() {
        Intent sharingIntent;
        if (pdf.getUri() == null) return;   // notify user?

        if (pdf.getUri().getScheme() != null && pdf.getUri().getScheme().startsWith("http"))
            sharingIntent = Utils.plainTextShareIntent(getString(R.string.share), pdf.getUri().toString());
        else
            sharingIntent = Utils.fileShareIntent(getString(R.string.share), pdf.getName(), pdf.getUri());

        startActivity(sharingIntent);
    }

    private void openSelectedDocument(Uri selectedDocumentUri) {
        if (selectedDocumentUri == null) return;

        if (pdf.getUri() == null || selectedDocumentUri.equals(pdf.getUri())) {
            pdf.setUri(selectedDocumentUri);
            displayFromUri(pdf.getUri());
        } else {
            Intent intent = new Intent(this, getClass());
            intent.setData(selectedDocumentUri);
            startActivity(intent);
        }
    }

    private void pickFile() {
        try {
            documentPickerLauncher.launch(new String[] { PDF.FILE_TYPE });
        } catch (ActivityNotFoundException e) {
            //alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    String computeHash() {
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            if (downloadedPdfFileContent != null) {
                int size = Math.min(PDF.HASH_SIZE, downloadedPdfFileContent.length);
                digester.update(downloadedPdfFileContent, 0, size);
            } else {
                InputStream inputStream = getContentResolver().openInputStream(pdf.getUri());
                byte[] buffer = new byte[PDF.HASH_SIZE];
                int amountRead = inputStream.read(buffer);
                if (amountRead == -1) {
                    return null;
                }
                digester.update(buffer, 0, amountRead);
            }
            return String.format("%032x", new BigInteger(1, digester.digest()));
        } catch (NoSuchAlgorithmException | IOException e) {
            return null;
        }
    }

    void configurePdfViewAndLoad(PDFView.Configurator viewConfigurator) {
        if (pdf.getPageNumber() == 0) { // attempt to find a saved location
            executor.execute(() -> { // off UI thread
                pdf.setContentHash(computeHash());
                Integer maybePageNumber = pdf.getContentHash() == null
                        ? Integer.valueOf(0)
                        : appDb.savedLocationDao().findSavedPage(pdf.getContentHash());
                handler.post(() -> // back on UI thread
                    configurePdfViewAndLoadWithPageNumber(
                        viewConfigurator,
                        maybePageNumber != null ? maybePageNumber : 0
                    )
                );
            });
        } else {
            configurePdfViewAndLoadWithPageNumber(viewConfigurator, pdf.getPageNumber());
        }
    }

    void configurePdfViewAndLoadWithPageNumber(PDFView.Configurator viewConfigurator, int pageNum) {
        PDFView pdfView = viewBinding.pdfView;
        configureTheme();

        pdfView.useBestQuality(pref.getHighQuality());
        pdfView.setMinZoom(Preferences.minZoomDefault);
        pdfView.setMidZoom(Preferences.midZoomDefault);
        pdfView.setMaxZoom(Preferences.maxZoomDefault);
        pdfView.zoomTo(pdf.getZoom());

        viewConfigurator
            .defaultPage(pageNum)
            .onPageChange(this::setCurrentPage)
            .enableAnnotationRendering(Preferences.annotationRenderingDefault)
            .enableAntialiasing(pref.getAntiAliasing())
            .onTap(this::toggleScrollAndButtonsVisibility)
            .scrollHandle(new DefaultScrollHandle(this))
            .spacing(Preferences.spacingDefault)
            .onError(this::handleFileOpeningError)
            .onPageError(this::reportLoadPageError)
            .pageFitPolicy(FitPolicy.WIDTH)
            .password(pdf.getPassword())
            .swipeHorizontal(pref.getHorizontalScroll())
            .autoSpacing(pref.getHorizontalScroll())
            .pageSnap(pref.getPageSnap())
            .pageFling(pref.getPageFling())
            .nightMode(pref.getPdfDarkTheme())
            .load();

        // Show the page scroll handler for 3 seconds when the pdf is loaded then hide it.
        pdfView.performTap();
        tappingHandler.postDelayed(() ->
                hideButtons(pdfView.getScrollHandle()), pref.getHideDelay());
    }

    private void configureTheme() {
        PDFView pdfView = viewBinding.pdfView;

        // set background color behind pages
        if (!pref.getPdfDarkTheme())
            pdfView.setBackgroundColor(Preferences.pdfDarkBackgroundColor);
        else
            pdfView.setBackgroundColor(Preferences.pdfLightBackgroundColor);

        if (pref.getAppFollowSystemTheme()) {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        else {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void reportLoadPageError(int page, Throwable error) {
        String message = getResources().getString(R.string.cannot_load_page) + page + " " + error;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }

    private void hideButtons(ScrollHandle handle) {
        /* TODO:
             the below removeCallbacksAndMessages will delete the timer for handle to be hidden
             which will cause the handle to be shown until the user taps the screen again */
        // stop any previous timer to hide them
        tappingHandler.removeCallbacksAndMessages(null);

//        if (handle != null) handle.activateHandlerHideDelayed();
        if (handle != null) handle.customHide();
        viewBinding.exitFullScreenButton.setVisibility(View.INVISIBLE);
        viewBinding.rotateScreenButton.setVisibility(View.INVISIBLE);
    }

    private boolean toggleScrollAndButtonsVisibility(MotionEvent event) {
        ScrollHandle handle = viewBinding.pdfView.getScrollHandle();
        LinearLayout exitButton = viewBinding.exitFullScreenButton;
        LinearLayout rotateButton = viewBinding.rotateScreenButton;

        if (handle == null) {
            toggleButtonsVisibility();
            return true;
        }

        // timer to hide them. This timer will be canceled in the else branch
        tappingHandler.removeCallbacksAndMessages(null);
        handle.cancelHideRunner();

        // set a new timer to hide
        tappingHandler.postDelayed(() -> {
            exitButton.setVisibility(View.INVISIBLE);
            rotateButton.setVisibility(View.INVISIBLE);
            handle.customHide();
        }, pref.getHideDelay());

        if (!handle.customShown()) {
            handle.customShow();
            if (pdf.isFullScreenToggled()) {
                exitButton.setVisibility(View.VISIBLE);
                rotateButton.setVisibility(View.VISIBLE);
            }
        }
        else if (exitButton.getVisibility() == View.GONE && pdf.isFullScreenToggled()) {
            exitButton.setVisibility(View.VISIBLE);
            rotateButton.setVisibility(View.VISIBLE);
        }
        else {
            hideButtons(handle);
        }
        return true;
    }

    private void toggleButtonsVisibility() {
        if (!pdf.isFullScreenToggled()) return;
        LinearLayout exitButton = viewBinding.exitFullScreenButton;
        LinearLayout rotateButton = viewBinding.rotateScreenButton;

        if (exitButton.getVisibility() == View.VISIBLE) {
            exitButton.setVisibility(View.INVISIBLE);
            rotateButton.setVisibility(View.INVISIBLE);
        }
        else {
            exitButton.setVisibility(View.VISIBLE);
            rotateButton.setVisibility(View.VISIBLE);
        }
    }

    private void handleFileOpeningError(Throwable exception) {
        if (exception instanceof PdfPasswordException) {
            if (pdf.getPassword() != null) {
                Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show();
                pdf.setPassword(null);  // prevent the toast if the user rotates the screen
            }
            askForPdfPassword();
        } else if (couldNotOpenFileDueToMissingPermission(exception)) {
            readFileErrorPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show();
            Log.e(TAG, getString(R.string.file_opening_error), exception);
        }
    }

    private boolean couldNotOpenFileDueToMissingPermission(Throwable e) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED)
            return false;

        String exceptionMessage = e.getMessage();
        return e instanceof FileNotFoundException &&
            exceptionMessage != null && exceptionMessage.contains(getString(R.string.permission_denied));
    }

    private void restartAppIfGranted(boolean isPermissionGranted) {
        if (isPermissionGranted) {
            // This is a quick and dirty way to make the system restart the current activity *and the current app process*.
            // This is needed because on Android 6 storage permission grants do not take effect until
            // the app process is restarted. // Mudelj: Why not just user recreate()?
            System.exit(0);
        } else {
            Toast.makeText(this, R.string.file_opening_error, Toast.LENGTH_LONG).show();
        }
    }

    private void toggleFullscreen(boolean fixFullScreen) {
        final View view = viewBinding.pdfView;
        if (!pdf.isFullScreenToggled() || fixFullScreen) {
            Objects.requireNonNull(getSupportActionBar()).hide();
            pdf.setFullScreenToggled(true);
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            // hide the scroll handle
            if (!fixFullScreen) {
                ScrollHandle handle = viewBinding.pdfView.getScrollHandle();
                if (handle != null) handle.customHide();
            }

            // show how to dialog
            if (pref.getShowFeaturesDialog()) showHowToExitFullscreenDialog();
        } else {
            Objects.requireNonNull(getSupportActionBar()).show();
            pdf.setFullScreenToggled(false);
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

    }

    void displayFromUri(Uri uri) {
        if (uri == null) {
            return;
        }

        pdf.setName(getFileName(uri));
        setTitle(pdf.getName());
        setTaskDescription(new ActivityManager.TaskDescription(pdf.getName()));

        String scheme = uri.getScheme();

        if (scheme != null && scheme.contains("http")) {
            downloadOrShowDownloadedFile(uri);
        } else {
            configurePdfViewAndLoad(viewBinding.pdfView.fromUri(uri));
        }
    }

    private void downloadOrShowDownloadedFile(Uri uri) {
        if (downloadedPdfFileContent == null) {
            downloadedPdfFileContent = (byte[]) getLastCustomNonConfigurationInstance();
        }
        if (downloadedPdfFileContent != null) {
            configurePdfViewAndLoad(viewBinding.pdfView.fromBytes(downloadedPdfFileContent));
        } else {
            // we will get the pdf asynchronously with the DownloadPDFFile object
            viewBinding.progressBar.setVisibility(View.VISIBLE);
            DownloadPDFFile downloadPDFFile = new DownloadPDFFile(this);
            downloadPDFFile.execute(uri.toString());
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return downloadedPdfFileContent;
    }

    public void hideProgressBar() {
        viewBinding.progressBar.setVisibility(View.GONE);
    }

    void saveToFileAndDisplay(byte[] pdfFileContent) {
        downloadedPdfFileContent = pdfFileContent;
        saveToDownloadFolderIfAllowed(pdfFileContent);
        configurePdfViewAndLoad(viewBinding.pdfView.fromBytes(pdfFileContent));
    }

    private void saveToDownloadFolderIfAllowed(byte[] fileContent) {
        if (Utils.canWriteToDownloadFolder(this)) {
            trySaveToDownloadFolder(fileContent, false);
        } else {
            saveToDownloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void trySaveToDownloadFolder(byte[] fileContent, boolean showSuccessMessage) {
        try {
            File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            Utils.writeBytesToFile(downloadDirectory, pdf.getName(), fileContent);
            if (showSuccessMessage) {
                Toast.makeText(this, R.string.saved_to_download, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, getString(R.string.save_to_download_failed), e);
            Toast.makeText(this, R.string.save_to_download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDownloadedFileAfterPermissionRequest(boolean isPermissionGranted) {
        if (isPermissionGranted) {
            trySaveToDownloadFolder(downloadedPdfFileContent, true);
        } else {
            Toast.makeText(this, R.string.save_to_download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    void navToSettings() {
        settingsLauncher.launch(new Intent(this, SettingsActivity.class));
    }

    private void setCurrentPage(int page, int pageCount) {
        String hash = pdf.getContentHash(); // Don't want fileContentHash to change out from under us
        if (hash != null)
            executor.execute(() -> // off UI thread
                appDb.savedLocationDao().insert(new SavedLocation(hash, pdf.getPageNumber()))
            );

        pdf.setPageNumber(page);
        pdf.setPageCount(viewBinding.pdfView.getPageCount());
        setTitle(pdf.getTitle());
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int indexDisplayName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (indexDisplayName != -1) {
                        result = cursor.getString(indexDisplayName);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, getString(R.string.error_load_file_name), e);
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private void printDocument() {
        mgr.print(pdf.getName(), new PdfDocumentAdapter(this, pdf.getUri()), null);
    }

    void askForPdfPassword() {
        PasswordDialogBinding dialogBinding = PasswordDialogBinding.inflate(getLayoutInflater());
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(R.string.protected_pdf)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    pdf.setPassword(dialogBinding.passwordInput.getText().toString());
                    displayFromUri(pdf.getUri());
                })
                .setIcon(R.drawable.lock_icon)
                .create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    void showPdfMetaDialog() {
        PdfDocument.Meta meta = viewBinding.pdfView.getDocumentMeta();
        if (meta != null) {
            Bundle dialogArgs = new Bundle();
            dialogArgs.putString(PdfMetaDialog.TITLE_ARGUMENT, meta.getTitle());
            dialogArgs.putString(PdfMetaDialog.AUTHOR_ARGUMENT, meta.getAuthor());
            dialogArgs.putString(PdfMetaDialog.CREATION_DATE_ARGUMENT, meta.getCreationDate());
            DialogFragment dialog = new PdfMetaDialog();
            dialog.setArguments(dialogArgs);
            dialog.show(getSupportFragmentManager(), "meta_dialog");
        }
    }

    void showHowToExitFullscreenDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getResources().getString(R.string.exit_fullscreen_title))
            .setMessage(getResources().getString(R.string.exit_fullscreen_message))
            .setPositiveButton(
                getResources().getString(R.string.exit_fullscreen_positive),
                (dialogInterface, i) -> pref.setShowFeaturesDialog(false)
            )
            .setNegativeButton(getResources().getString(R.string.ok), (dialogInterface, i) -> dialogInterface.dismiss())
            .create()
            .show();
    }

    private void showAppFeaturesDialogOnFirstRun() {
        if (pref.getShowFeaturesDialog()) {
            new Handler().postDelayed(() -> showAppFeaturesDialog(this), 500);
            pref.setShowFeaturesDialog(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_about) {
            startActivity(Utils.navIntent(this, AboutActivity.class));
        }
        else if (itemId == R.id.theme) {
            startActivity(Utils.navIntent(getApplicationContext(), SettingsActivity.class));
        }
        else if (itemId == R.id.settings) { navToSettings(); }
        else if (itemId == R.id.share_file) { if (pdf.getUri() != null) shareFile(); }
        else if (itemId == R.id.fullscreen_option) { toggleFullscreen(false); }
        else if (itemId == R.id.switch_theme) { switchPdfTheme(); }
        else if (itemId == R.id.open_file)  { pickFile(); }
        else if (itemId == R.id.meta_data) { if (pdf.getUri() != null) showPdfMetaDialog(); }
        else if (itemId == R.id.print_file) { printDocument(); }
        else { return super.onOptionsItemSelected(item); }

        return true;
    }

    private void switchPdfTheme() {
        pref.setPdfDarkTheme(!pref.getPdfDarkTheme());
        //configurePdfViewAndLoadWithPageNumber(viewBinding.pdfView.fromUri(uri), pageNumber);
        recreate();
    }

    public static class PdfMetaDialog extends DialogFragment {

        public static final String TITLE_ARGUMENT = "title";
        public static final String AUTHOR_ARGUMENT = "author";
        public static final String CREATION_DATE_ARGUMENT = "creation_date";

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            return builder.setTitle(R.string.meta)
                .setMessage(getString(R.string.pdf_title, getArguments().getString(TITLE_ARGUMENT)) + "\n" +
                        getString(R.string.pdf_author, getArguments().getString(AUTHOR_ARGUMENT)) + "\n" +
                        getString(R.string.pdf_creation_date, getArguments().getString(CREATION_DATE_ARGUMENT)))
                .setPositiveButton(R.string.ok, (dialog, which) -> {})
                .setIcon(R.drawable.info_icon)
                .create();
        }
    }


    private final ActivityResultLauncher<String[]> documentPickerLauncher = registerForActivityResult(
            new OpenDocument(),
            this::openSelectedDocument
    );

    private final ActivityResultLauncher<String> saveToDownloadPermissionLauncher = registerForActivityResult(
            new RequestPermission(),
            this::saveDownloadedFileAfterPermissionRequest
    );

    private final ActivityResultLauncher<String> readFileErrorPermissionLauncher = registerForActivityResult(
            new RequestPermission(),
            this::restartAppIfGranted
    );

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new StartActivityForResult(),
            result -> displayFromUri(pdf.getUri())
    );
}

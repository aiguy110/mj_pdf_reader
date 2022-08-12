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

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.data.Preferences
import com.gitlab.mudlej.MjPdfReader.databinding.PasswordDialogBinding
import com.shockwave.pdfium.PdfDocument

fun showAppFeaturesDialog(context: Context) {
    val end = "\n\n"
    AlertDialog.Builder(context)
        .setTitle(context.resources.getString(R.string.app_name) + " Features")
        .setMessage(
            "* Fast & smooth experience." + end +
                    "* Minimalist & simple user interface." + end +
                    "* Remembers the last opened page." + end +
                    "* Dark mode for the app and the PDF." + end +
                    "* True full screen with hidable buttons." + end +
                    "* An option to keep the screen on." + end +
                    "* Open online PDFs through links." + end +
                    "* Share & print PDFs." + end +
                    "* Open multiple PDFs." + end +
                    "* FOSS and totally private. (see About)."
        )
        .setPositiveButton(
            context.resources.getString(R.string.ok)
        ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
        .create()
        .show()
}

fun showMetaDialog(context: Context, meta: PdfDocument.Meta) {
    AlertDialog.Builder(context)
        .setTitle(R.string.meta)
        .setMessage(
            "${context.getString(R.string.pdf_title)}: ${meta.title}\n" +
            "${context.getString(R.string.pdf_author)}: ${meta.author}\n" +
            "${context.getString(R.string.pdf_creation_date)}: ${meta.creationDate.format()}\n"
        )
        .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
        .setIcon(R.drawable.info_icon)
        .create()
        .show()
}


fun showHowToExitFullscreenDialog(context: Context, pref: Preferences) {
    AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.exit_fullscreen_title))
        .setMessage(context.getString(R.string.exit_fullscreen_message))
        .setPositiveButton(context.getString(R.string.exit_fullscreen_positive)) { _, _ ->
            pref.setShowFeaturesDialog(false)
        }
        .setNegativeButton(context.getString(R.string.ok)) {
                dialog: DialogInterface, _ -> dialog.dismiss()
        }
        .create()
        .show()
}


fun showAskForPasswordDialog(
    context: Context,
    pdf: PDF,
    dialogBinding: PasswordDialogBinding,
    displayFunc: (Uri?) -> Unit)
{
    val alert = AlertDialog.Builder(context)
        .setTitle(R.string.protected_pdf)
        .setView(dialogBinding.root)
        .setIcon(R.drawable.lock_icon)
        .setPositiveButton(R.string.ok) { _, _ ->
            pdf.password = dialogBinding.passwordInput.text.toString()
            displayFunc(pdf.uri)
        }
        .create()

    alert.setCanceledOnTouchOutside(false)
    alert.show()
}
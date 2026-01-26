package com.example.swapit.ui.additem

import android.net.Uri

sealed class EditableImage {
    data class Existing(val url: String) : EditableImage()
    data class New(val uri: Uri) : EditableImage()
}

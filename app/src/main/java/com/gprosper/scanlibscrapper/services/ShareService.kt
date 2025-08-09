package com.gprosper.scanlibscrapper.services

import android.content.Context
import android.content.Intent

object ShareService {
    private const val MYJD_PACKAGE = "org.appwork.myjdandroid" // if not found, we fall back

    fun shareLinksToMyJD(context: Context, links: List<String>) {
        val payload = links.joinToString("\n")
        val pm = context.packageManager
        val direct = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, payload)
            `package` = MYJD_PACKAGE
        }

        // Verify the target exists; otherwise show chooser
        if (direct.resolveActivity(pm) != null) {
            context.startActivity(direct)
        } else {
            // Fallback
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, payload)
                },
                "Send to…"
            )
            context.startActivity(chooser)
        }
    }
}
package com.gprosper.scanlibscrapper.model

import pl.droidsonroids.jspoon.annotation.Selector

class Page() {

    @Selector(".rewlink", attr = "href")
    lateinit var linkList: List<String>
}
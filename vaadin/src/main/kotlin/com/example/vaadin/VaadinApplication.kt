package com.example.vaadin

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.PWA
import com.vaadin.flow.server.startup.ServletContextListeners

@Route("")
class MainView : Div() {
    init {
        text = "FUI Vaadin UI - hello"
    }
}

fun main() {
    println("Run Vaadin with embedded server separately")
}


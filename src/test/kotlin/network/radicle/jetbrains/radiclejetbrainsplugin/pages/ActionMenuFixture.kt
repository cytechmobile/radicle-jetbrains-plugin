// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package network.radicle.jetbrains.radiclejetbrainsplugin.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor
import java.time.Duration

fun RemoteRobot.actionMenu(menuText: String, parentMenuText: String): ActionMenuFixture {
    val path =
        if (parentMenuText.isNotEmpty())
            "//div[@class='ActionMenu' and @text='$parentMenuText']//div[@text='$menuText']"
        else
            "//div[@class='ActionMenu' and @text='$menuText']"

    val xpath = byXpath("text '$menuText'", path)
    waitFor {
        findAll<ActionMenuFixture>(xpath).isNotEmpty()
    }
    return findAll<ActionMenuFixture>(xpath).first()
}

fun RemoteRobot.actionMenuItem(text: String): ActionMenuItemFixture {
    val xpath = byXpath("text '$text'", "//div[@class='ActionMenuItem' and @text='$text']")
    waitFor (duration = Duration.ofSeconds(60)) {
        findAll<ActionMenuItemFixture>(xpath).isNotEmpty()
    }
    return findAll<ActionMenuItemFixture>(xpath).first()
}

@FixtureName("ActionMenu")
class ActionMenuFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : ComponentFixture(remoteRobot, remoteComponent)

@FixtureName("ActionMenuItem")
class ActionMenuItemFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : ComponentFixture(remoteRobot, remoteComponent)
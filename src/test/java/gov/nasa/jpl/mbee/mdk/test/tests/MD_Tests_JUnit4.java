package gov.nasa.jpl.mbee.mdk.test.tests;

import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.plugins.PluginUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(MyMagicDrawTestRunner.class)
public class MD_Tests_JUnit4 {
    public MD_Tests_JUnit4() {
        System.out.println("INSTANTIATED MD_TESTS_JUNIT4");
    }

    public static void main(String... args) {
        System.out.println("HELLO WORLD!");
    }

    @Test
    public void name() throws Exception {
        File file = new File("/Users/igomes/Documents/Scratchpad_Donbot.mdzip");
        //Project mProject = openProject(file.getAbsolutePath());
        //System.out.println("Project: " + mProject);

        //System.out.println("LOG_JSONN: " + MDKOptionsGroup.getMDKOptions().isLogJson());
        System.out.println("Plugin Count - " + PluginUtils.getPlugins().size());
        PluginUtils.getPlugins().stream().map(Plugin::getDescriptor).forEach(descriptor -> System.out.println(descriptor.getName() + " v" + descriptor.getVersion() + " by " + descriptor.getProvider()));
        //setDoNotUseSilentMode(true);
        Assert.assertTrue(true);
    }
}
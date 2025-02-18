package ch.admin.bit.jeap.processcontext.ui.configuration;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Version Detector based on the Location. Here we find the Version of the jEAP-Process-Context-Service.
 * Served for the displaying in UI.
 */
@Slf4j
public class VersionDetector {

    public String getVersion() {
        String codeSourceLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
        return extractVersionFromString(codeSourceLocation);
    }

    /**
     * Extracts the Version Number in the form the ManifestPath.
     * @param codeSourceLocationURL Path of the CodeSource, includes the Version Number
     * @return Version Number
     */
    protected String extractVersionFromString(String codeSourceLocationURL) {
        Pattern pattern = Pattern.compile("\\d+(?:\\.\\d+){1,3}(?:-SNAPSHOT)?");
        Matcher matcher = pattern.matcher(codeSourceLocationURL);
        if (matcher.find()) {
            StringBuilder stringBuilder = new StringBuilder(codeSourceLocationURL);
            return stringBuilder.substring(matcher.start(), matcher.end());
        } else return "??";
    }
}

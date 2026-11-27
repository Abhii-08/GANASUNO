import re, unittest
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
SOURCE = (MAIN / "java/com/abhi/ganasuno/MainActivity.kt").read_text()
class PrimaryColorFix(unittest.TestCase):
    def test_no_static_primary_attribute(self):
        for p in MAIN.rglob("*.kt"):
            self.assertNotRegex(p.read_text(), r"\bR\.attr\.colorPrimary\b", str(p))
    def test_both_call_sites(self):
        self.assertIn("glassDock.tabs(", SOURCE)
        self.assertIn("setTextColor(primaryUiColor(this))", SOURCE)
    def test_uppercase(self):
        self.assertIn("group.uppercase(java.util.Locale.ROOT)", SOURCE)
        self.assertNotIn("upypercase", SOURCE)
    def test_fallback_resources(self):
        for folder in ["values", "values-night"]:
            colors = ET.parse(MAIN / "res" / folder / "colors.xml").getroot()
            self.assertTrue(any(c.get("name") == "brand_primary" for c in colors))
        self.assertIn("if (attrId == 0) return fallback", SOURCE)
        self.assertIn("resolveAttribute(attrId, value, true)", SOURCE)
    def test_version(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text()
        self.assertIn('versionName = "45.0"', gradle)
if __name__ == "__main__": unittest.main()

package com.max480.randomstuff.gae;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servlet allowing to generate bitmap fonts for usage in Celeste (~~and any other game using the XML output of BMFont actually~~).
 */
@WebServlet(name = "CelesteFontGeneratorService", urlPatterns = {"/celeste/font-generator"})
@MultipartConfig
public class CelesteFontGeneratorService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("CelesteFontGeneratorService");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("error", false);
        request.setAttribute("badrequest", false);
        request.setAttribute("nothingToDo", false);
        request.getRequestDispatcher("/WEB-INF/font-generator.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("error", false);
        request.setAttribute("badrequest", false);
        request.setAttribute("nothingToDo", false);
        boolean sentZip = false;

        if (!ServletFileUpload.isMultipartContent(request)) {
            // if not, we stop here
            request.setAttribute("badrequest", true);
            response.setStatus(400);
        } else {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setRepository(new File("/tmp"));
            ServletFileUpload upload = new ServletFileUpload(factory);

            // parse request
            String font = null;
            String fontFileName = null;
            String dialogFile = null;

            String customFontFileName = null;
            InputStream customFontFile = null;
            try {
                for (FileItem item : upload.parseRequest(request)) {
                    if (item.isFormField()) {
                        // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                        String fieldname = item.getFieldName();
                        String fieldvalue = item.getString();

                        if ("font".equals(fieldname)) {
                            font = fieldvalue;
                        } else if ("fontFileName".equals(fieldname)) {
                            fontFileName = fieldvalue;
                        }
                    } else {
                        // Process form file field (input type="file").
                        String fieldname = item.getFieldName();
                        InputStream filecontent = item.getInputStream();

                        if ("dialogFile".equals(fieldname)) {
                            dialogFile = IOUtils.toString(filecontent, StandardCharsets.UTF_8);
                        } else if ("fontFile".equals(fieldname)) {
                            customFontFileName = item.getName();
                            if (customFontFileName.replace("\\", "/").contains("/")) {
                                customFontFileName = customFontFileName.substring(customFontFileName.replace("\\", "/").indexOf("/") + 1);
                            }
                            customFontFile = filecontent;
                        }
                    }
                }
            } catch (FileUploadException e) {
                logger.warning("Cannot parse request: " + e);
            }

            if (font == null || fontFileName == null || dialogFile == null || hasForbiddenCharacter(fontFileName)
                    || (font.equals("custom") && (customFontFileName == null || hasForbiddenCharacter(customFontFileName)))) {

                // parameter missing, or font file name has illegal characters
                request.setAttribute("badrequest", true);
                response.setStatus(400);
            } else {
                try {
                    // we can try generating the font now!
                    byte[] result;
                    if (font.equals("custom")) {
                        result = generateFont(fontFileName, customFontFile, customFontFileName, dialogFile);
                    } else {
                        result = generateFont(fontFileName, font, dialogFile);
                    }

                    if (result.length == 0) {
                        // well... we didn't generate anything.
                        request.setAttribute("nothingToDo", true);
                    } else {
                        // sent the zip to the user.
                        response.setContentType("application/zip");
                        response.setContentLength(result.length);
                        response.setHeader("Content-Disposition", "attachment; filename=\"celeste-bitmap-font.zip\"");
                        IOUtils.write(result, response.getOutputStream());
                        sentZip = true;
                    }
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    // something blew up along the way!
                    logger.severe("Could not generate font!");
                    e.printStackTrace();

                    request.setAttribute("error", true);
                    response.setStatus(500);
                }
            }
        }

        if (!sentZip) {
            // render the HTML page.
            request.getRequestDispatcher("/WEB-INF/font-generator.jsp").forward(request, response);
        }
    }

    /**
     * Checks if a file name contains characters that are forbidden in file names.
     */
    private static boolean hasForbiddenCharacter(String name) {
        return name.contains("/") || name.contains("\\") || name.contains("*") || name.contains("?")
                || name.contains(":") || name.contains("\"") || name.contains("<") || name.contains(">")
                || name.contains("|") || name.contains("\r") || name.contains("\n");
    }

    /**
     * Generates a font from a custom font provided by input stream.
     *
     * @param fontFileName         The base name for the fnt and png files
     * @param font                 The font to use as an input stream
     * @param uploadedFontFileName The name of the font that is provided as an input stream
     * @param dialogFile           The dialog file to take characters from
     * @return The zip containing all the files, as a byte array. Can be empty if no charater was exported
     */
    private static byte[] generateFont(String fontFileName, InputStream font, String uploadedFontFileName, String dialogFile) throws IOException {
        // create a temp dir to dump stuff to.
        final Path tempDirectory = Files.createTempDirectory("celeste-font-generator-");

        // write the font to that folder.
        final File fontFile = tempDirectory.resolve(uploadedFontFileName).toFile();
        try (OutputStream os = new FileOutputStream(fontFile)) {
            IOUtils.copy(font, os);
        }

        // generate the bitmap font!
        boolean hasContent = generateFont(fontFileName + "_image", fontFileName + ".fnt", fontFile, dialogFile, new HashSet<>());

        // if we generated no font, then return now.
        if (!hasContent) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
            return new byte[0];
        }

        // delete the font
        Files.delete(fontFile.toPath());

        // and zip the whole thing.
        return zipAndDeleteTempDirectory(tempDirectory);
    }

    /**
     * Generates a font from one of Celeste's base fonts (that depends on language).
     * Only characters absent from the game's font are exported.
     *
     * @param fontFileName The base name for the png files
     * @param language     One of "russian", "japanese", "korean", "chinese" or "renogare" to pick the font to use and the name for the fnt file
     * @param dialogFile   The dialog file to take characters from
     * @return The zip containing all the files, as a byte array. Can be empty if no charater was exported
     */
    private static byte[] generateFont(String fontFileName, String language, String dialogFile)
            throws IOException, ParserConfigurationException, SAXException {

        String fontName;
        String vanillaFntName;
        switch (language) {
            case "russian":
                fontName = "Noto Sans Med.ttf";
                vanillaFntName = "russian.fnt";
                break;
            case "japanese":
                fontName = "Noto Sans CJK JP Medium.otf";
                vanillaFntName = "japanese.fnt";
                break;
            case "korean":
                fontName = "Noto Sans CJK KR Medium.otf";
                vanillaFntName = "korean.fnt";
                break;
            case "chinese":
                fontName = "Noto Sans CJK SC Medium.otf";
                vanillaFntName = "chinese.fnt";
                break;
            default:
                fontName = "Renogare.otf";
                vanillaFntName = "renogare64.fnt";
                break;
        }

        // create a temp dir to dump stuff to.
        final Path tempDirectory = Files.createTempDirectory("celeste-font-generator-");

        // extract the font from classpath to that folder.
        final File fontFile = tempDirectory.resolve(fontName).toFile();
        try (InputStream is = CelesteFontGeneratorService.class.getClassLoader().getResourceAsStream("font-generator/fonts/" + fontName);
             OutputStream os = new FileOutputStream(fontFile)) {

            IOUtils.copy(is, os);
        }

        // extract the font XML from classpath to that folder.
        final File fontXml = tempDirectory.resolve(vanillaFntName).toFile();
        try (InputStream is = CelesteFontGeneratorService.class.getClassLoader().getResourceAsStream("font-generator/vanilla/" + vanillaFntName);
             OutputStream os = new FileOutputStream(fontXml)) {

            IOUtils.copy(is, os);
        }

        // parse the XML and delete it
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(fontXml);
        Files.delete(fontXml.toPath());

        // get the list of existing codes
        Set<Integer> existingCodes = new HashSet<>();
        NodeList chars = document.getElementsByTagName("char");
        for (int i = 0; i < chars.getLength(); i++) {
            Node charItem = chars.item(i);
            existingCodes.add(Integer.parseInt(charItem.getAttributes().getNamedItem("id").getNodeValue()));
        }

        // generate the bitmap font!
        boolean hasContent = generateFont(fontFileName, vanillaFntName, fontFile, dialogFile, existingCodes);

        // if we generated no font, then return now.
        if (!hasContent) {
            FileUtils.deleteDirectory(tempDirectory.toFile());
            return new byte[0];
        }

        // delete the font
        Files.delete(fontFile.toPath());

        // and zip the whole thing.
        return zipAndDeleteTempDirectory(tempDirectory);
    }

    /**
     * Generates a font from a font file on disk.
     *
     * @param fontFileName  The base name for the png files
     * @param fntName       The name for the fnt file
     * @param font          The font file
     * @param dialogFile    The dialog file to take characters from
     * @param existingCodes Code points for the characters to exclude from the export
     * @return true if characters were exported, false otherwise.
     */
    private static boolean generateFont(String fontFileName, String fntName, File font, String dialogFile, Set<Integer> existingCodes) throws IOException {
        // take all characters that do not exist and jam them all into a single string
        final String missingCharacters = dialogFile.codePoints()
                .filter(c -> !existingCodes.contains(c))
                .mapToObj(c -> new String(new int[]{c}, 0, 1))
                .distinct()
                .filter(s -> !"\n".equals(s) && !"\r".equals(s))
                .collect(Collectors.joining());

        final Path directory = font.toPath().getParent().toAbsolutePath();

        // generate the font using libgdx
        Semaphore waitUntilFinished = new Semaphore(0);
        AtomicBoolean failure = new AtomicBoolean(false);
        AtomicBoolean empty = new AtomicBoolean(false);

        final HeadlessApplication app = new HeadlessApplication(new ApplicationAdapter() {
            public void create() {
                try {
                    BitmapFontWriter.FontInfo info = new BitmapFontWriter.FontInfo();
                    info.padding = new BitmapFontWriter.Padding(1, 1, 1, 1);
                    info.face = font.getName().substring(0, font.getName().lastIndexOf("."));
                    info.size = 64;
                    info.aa = 4;
                    info.spacing.horizontal = 1;
                    info.spacing.vertical = 1;

                    FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
                    param.size = 43;
                    param.spaceX = 1;
                    param.spaceY = 1;
                    param.characters = missingCharacters;
                    param.packer = new HackPixmapPacker(256, 256, Pixmap.Format.RGBA8888, 1, false, new HackSkylineStrategy());

                    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(new NoMapFileHandle(font));
                    FreeTypeFontGenerator.FreeTypeBitmapFontData data = generator.generateData(param);

                    final Array<PixmapPacker.Page> pages = param.packer.getPages();
                    Pixmap[] pix = new Pixmap[pages.size];
                    for (int i = 0; i < pages.size; i++) {
                        pix[i] = pages.get(i).getPixmap();
                    }

                    if (pix.length == 0) {
                        empty.set(true);
                        waitUntilFinished.release();
                        return;
                    }

                    BitmapFontWriter.setOutputFormat(BitmapFontWriter.OutputFormat.XML);
                    BitmapFontWriter.writeFont(data, pix, Gdx.files.absolute(directory.resolve(fontFileName + ".fnt").toString()), info);

                    generator.dispose();
                    data.dispose();
                    param.packer.dispose();

                    waitUntilFinished.release();
                } catch (Exception e) {
                    logger.severe("Could not generate font");
                    e.printStackTrace();
                    failure.set(true);
                    waitUntilFinished.release();
                }
            }
        });

        waitUntilFinished.acquireUninterruptibly();
        app.exit();
        if (empty.get()) {
            return false;
        }
        if (failure.get()) {
            throw new IOException("Generating font failed!");
        }

        // rename the fnt file as required
        Files.move(directory.resolve(fontFileName + ".fnt"), directory.resolve(fntName));

        // and... done!
        return true;
    }

    /**
     * Zips the given directory, then deletes it. Does not support subfolders, but it is not needed anyway.
     */
    private static byte[] zipAndDeleteTempDirectory(Path tempDirectory) throws IOException {
        // zip the whole folder.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            for (File f : tempDirectory.toFile().listFiles()) {
                zipOutput.putNextEntry(new ZipEntry(f.getName()));
                try (FileInputStream fileInput = new FileInputStream(f)) {
                    IOUtils.copy(fileInput, zipOutput);
                }
            }
        }

        // delete it.
        FileUtils.deleteDirectory(tempDirectory.toFile());

        // and... done!
        return output.toByteArray();
    }

    /**
     * The map() method from FileHandle seems to be causing the file to always stay open, preventing its cleanup...
     * so we want to nope it out.
     * GdxRuntimeException is expected by FreeTypeFontGenerator, and the fallback it uses does not keep the file open.
     */
    private static class NoMapFileHandle extends FileHandle {
        public NoMapFileHandle(File file) {
            super(file);
        }

        public ByteBuffer map() {
            throw new GdxRuntimeException("nope");
        }
    }

    /*
     * The following is pretty hacky:
     * basically, FreeTypeFontGenerator always expects characters to be added to the last page, except the strategy
     * can add those to previous pages if it fits.
     * So, we temporarily remove pages from the pixmap packer until FreeTypeFontGenerator reads it, so that glyphs
     * get associated to the right page in the fnt file.
     * ... yeah.
     */

    private static class HackSkylineStrategy extends PixmapPacker.SkylineStrategy {
        @Override
        public PixmapPacker.Page pack(PixmapPacker packer, String name, Rectangle rect) {
            PixmapPacker.Page p = super.pack(packer, name, rect);

            int pageIndex = packer.getPages().indexOf(p, true);
            if (packer.getPages().size - 1 > pageIndex) {
                // character wasn't added to the last page! remove pages until we get it.
                Array<PixmapPacker.Page> pages = new Array<>(packer.getPages());
                while (packer.getPages().size - 1 > pageIndex) {
                    packer.getPages().pop();
                }
                // save the actual page list to restore it later.
                ((HackPixmapPacker) packer).actualPages = pages;
            }

            return p;
        }
    }

    private static class HackPixmapPacker extends PixmapPacker {
        private Array<Page> actualPages;

        public HackPixmapPacker(int pageWidth, int pageHeight, Pixmap.Format pageFormat, int padding, boolean duplicateBorder, PackStrategy packStrategy) {
            super(pageWidth, pageHeight, pageFormat, padding, duplicateBorder, packStrategy);
        }

        @Override
        public Array<Page> getPages() {
            if (actualPages == null) {
                return super.getPages();
            } else {
                // restore the actual list of pages, and return the faked one.
                Array<Page> result = new Array<>(super.getPages());
                super.getPages().clear();
                super.getPages().addAll(actualPages);
                actualPages = null;
                return result;
            }
        }
    }
}
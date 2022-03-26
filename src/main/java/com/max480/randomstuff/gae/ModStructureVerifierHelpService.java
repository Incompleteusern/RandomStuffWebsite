package com.max480.randomstuff.gae;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * When the Mod Structure Verifier finds issues with a mod zip, it sends a link to this page with parameters
 * corresponding to the bot settings and to the issues it found, to give more help to the user.
 */
@WebServlet(name = "ModStructureVerifierHelp", urlPatterns = {"/celeste/mod-structure-verifier-help"})
@MultipartConfig
public class ModStructureVerifierHelpService extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher("/WEB-INF/mod-structure-verifier-help.jsp").forward(request, response);
    }
}
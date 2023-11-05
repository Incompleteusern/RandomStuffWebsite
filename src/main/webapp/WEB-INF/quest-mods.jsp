<%@ page import="java.util.List, ovh.maddie480.randomstuff.frontend.quest.Mod, java.util.Arrays, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<script src="https://code.jquery.com/jquery-3.7.1.min.js"
    integrity="sha256-/JqT3SQfawRcv/BIHPThkBvs0OEvtFFmqPF/lYI/Cxo=" crossorigin="anonymous"></script>

<h1>Liste des mods de Quest</h1>

<% if((boolean) request.getAttribute("error")) { %>
    <div class="alert alert-danger">Une erreur est survenue lors du
        chargement de la page.
        Veuillez r�essayer dans quelques minutes, ou contacter Maddie si l'erreur persiste.
    </div>
<% } else { %>
    <div class="row">
        <% for(Mod mod : (List<Mod>) request.getAttribute("mods")) { %>
            <div class="col-lg-4 col-md-6 px-3 py-3">
                <div class="card">
                    <% if(mod.getImageUrl() != null && !mod.getImageUrl().isEmpty()) { %>
                            <img src="<%= escapeHtml4(mod.getImageUrl()) %>" class="card-img-top" alt="Image du mod">
                    <% } %>
                    <div class="card-body">
                        <h5 class="card-title"><%= escapeHtml4(mod.getName()) %></h5>
                        <h6 class="card-subtitle mb-2 text-muted">
                            par <%= escapeHtml4(mod.getAuthor()) %> - v<%= escapeHtml4(mod.getVersion()) %>
                            <% if(mod.isNeedTexmod()) { %>
                            <span class="badge bg-primary" data-toggle="tooltip" data-placement="top"
                                title="Ce mod a besoin du Texmod pour fonctionner.">Texmod</span>
                            <% } %>
                            <% if(mod.isHasCheckpointSupport()) { %>
                            <span class="badge bg-success" data-toggle="tooltip" data-placement="top"
                                title="Ce mod poss�de des checkpoints, permettant de ne pas reprendre du d�but � chaque partie.">Checkpoints</span>
                            <% } %>
                            <% if(Arrays.asList("152091059537575936corruptionparquesttools", "354341658352943115corruptionpartexmodeditor").contains(mod.getId())) { %>
                            <span class="badge bg-warning" data-toggle="tooltip" data-placement="top"
                                title="Ce mod n�cessite une proc�dure sp�cifique pour �tre appliqu� � Quest.">Sp�cial</span>
                            <% } %>
                            <% if(mod.isHasSpeedmod()) { %>
                            <span class="badge bg-danger" data-toggle="tooltip" data-placement="top"
                                title="Ce mod modifie la vitesse des personnages (n�cessite le Mod Manager pour fonctionner).">Vitesse</span>
                            <% } %>
                        </h6>
                        <p class="card-text"><%= mod.getDescription() %></p>
                        <a href="<%= escapeHtml4(mod.getModUrl()) %>" class="btn btn-success" target="_blank">T�l�charger</a>

                        <% if(mod.getWebPage() != null && !mod.getWebPage().isEmpty()) { %>
                            <a href="<%= escapeHtml4(mod.getWebPage()) %>" class="btn btn-link" target="_blank">Page Web</a>
                        <% } %>

                        <!-- Permalink = https://maddie480.ovh/quest/mods/<%= mod.getId() %> -->
                    </div>
                </div>
            </div>
        <% } %>
    </div>
<% } %>

<script src="js/mods.js"></script>

package recherche_film;

import logger.CompositeLogger;
import logger.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

public class RechercheFilm {

    private CompositeLogger log;
    private Connection JDBCConn;
    private ArrayList<String> vars;


    public static void main(String[] args)  {

            Scanner scan = new Scanner(System.in);
            System.out.println("Entrez votre requête :\n");
            String request;
            while(! (request=scan.nextLine()).equals("q")){
                RechercheFilm RF = null;
                RF = new RechercheFilm("bdfilm.sqlite");
                RF.log.info("test", "request = "+request);
                RF.retrouve(request);
                RF.fermeBase();





        }


    }

    //TODO : combinaisons, traitement du résultat de la requête et transformation en JSON

    RechercheFilm(String nomFicherSQLite)  {

        try {
            this.log = LoggerFactory.getConsLogger("log.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.JDBCConn = null;
        this.vars = new ArrayList<>();
        try {

            // db parameters
            String url = "jdbc:sqlite:C:\\Projet_Genie_Logiciel\\bdfilm.sqlite"/*+nomFicherSQLite*/;

            // create a connection to the database
            JDBCConn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.exit(1);

        }
    }

    public void fermeBase() {
        try {
            if (JDBCConn != null) {
                JDBCConn.close();
                log.info("JDBC", "Connection closed");
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public String retrouve(String RQS)  {
        StringBuilder jsonreturn = new StringBuilder();
        String[] parts;
        vars = new ArrayList<>();
        ArrayList<InfoFilm> films = new ArrayList<>();

        StringBuilder request = new StringBuilder("with filtre as (\n");
         /* StringBuilder request = new StringBuilder("with filtre as ( select id_film from recherche_titre where titre match 'vol' )\n");
         */
        String flatRequestPart = "select f.id_film, f.titre, f.duree, f.annee, py.nom, p.prenom, p.nom, g.role," +
                " group_concat(a.titre, '|') as autres_titres\n" +
                "from filtre\n" +
                "join films f\n" +
                "on f.id_film = filtre.id_film\n" +
                "join pays py\n" +
                "on py.code=f.pays\n" +
                "left join autres_titres a\n" +
                "on a.id_film=f.id_film\n" +
                "join generique g\n" +
                "on g.id_film=f.id_film\n" +
                "join personnes p\n" +
                "on p.id_personne=g.id_personne\n" +
                "group by f.id_film, f.titre, f.duree, f.annee, py.nom, p.prenom, p.nom, g.role";

        //RQS = RQS.toLowerCase();
        parts = RQS.split(",");
        ArrayList<String> Parts = new ArrayList<>(Arrays.asList(parts));
        log.info("info", "Number of statements : "+parts.length);

        for(int i = 0; i<parts.length; i++){
            /*CheckConditions and add to request (check methods must return the string to add to the request) */
            request.append(checkConditionAndReturnsSQL(Parts.get(i)));
            if((parts.length > 1) && i != parts.length - 1) request.append("\nintersect\n");
        }


        request.append(" )\n"+flatRequestPart);
        log.info(",fjn", "values found : "+vars.toString());

        try (PreparedStatement stmt  = JDBCConn.prepareStatement(request.toString())){
            int i;
            for (i=1; i<vars.size()+1; i++) {
                log.info("test", "arg"+i+" passed to PrepStat: "+vars.get(i-1));
                stmt.setString(i, vars.get(i-1));
            }
            log.info("retrouve", "Request : \n"+request.toString());
            ResultSet rs = stmt.executeQuery();
            log.info("info", "Request executed");
            //log.info("test", "Film 1 : "+rs.getString(1)+rs.getString(2)+rs.getString(3)+rs.getString(4)+rs.getString(5)+rs.getString(6)+rs.getString(7)+rs.getString(8)+rs.getString(9));
            // loop through the result set
            int previousId_film=0;
                ArrayList<NomPersonne> reals = new ArrayList<>();
                ArrayList<NomPersonne> acteurs = new ArrayList<>();
                String titre=null,pays=null;
                int annee=-1,duree=-1;
            ArrayList<String> autres_titres=null;



            while (rs.next() && films.size() <= 100) {
                    if(rs.getInt("id_film") == previousId_film){
                        if(rs.getString("role").equals("R"))
                            reals.add(new NomPersonne(rs.getString("nom"), rs.getString("prenom")));
                        if(rs.getString("role").equals("A"))
                            acteurs.add(new NomPersonne(rs.getString("nom"), rs.getString("prenom")));
                    }
                    else {
                        if(!rs.isFirst()){
                            log.info("qidj", "Adding "+titre+" to the results");
                            films.add(new InfoFilm(titre, reals, acteurs, pays, annee, duree, autres_titres));
                            reals = new ArrayList<>();
                            acteurs = new ArrayList<>();
                            titre=null;
                            pays=null;
                            annee=-1;
                            duree=-1;
                            autres_titres=null;
                        }
                        titre=rs.getString("titre");
                        previousId_film=rs.getInt("id_film");
                        pays=rs.getString("pays");
                        annee=rs.getInt("annee");
                        duree=rs.getInt("duree");
                        String[] titres = rs.getString("autres_titres").split("\\|");
                        autres_titres = new ArrayList<>(Arrays.asList(titres));
                        if(rs.getString("role").equals("R"))
                            reals.add(new NomPersonne(rs.getString("nom"), rs.getString("prenom")));
                        if(rs.getString("role").equals("A"))
                            acteurs.add(new NomPersonne(rs.getString("nom"), rs.getString("prenom")));
                       }

                for (InfoFilm film : films) {
                    System.out.println(film.toString());
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.exit(1);
            e.printStackTrace();
        }


        ArrayList<NomPersonne> real= new ArrayList<>();
        ArrayList<NomPersonne> acteurs = new ArrayList<>();
        //real.add(new NomPersonne("Guigui", "zblu"));
        //acteurs.add(new NomPersonne("Guigui", "zblui"));
        ArrayList<String> titres = new ArrayList<>();
        //titres.add("AKIKI");


        InfoFilm film = new InfoFilm("hello",real, acteurs, "france", 1998, 115,  titres);
        InfoFilm film2 = new InfoFilm("nulos",real, acteurs, "null", 12, 0,  titres);
        InfoFilm film3 = new InfoFilm("ololo",real, acteurs, "null", 12, 0,  titres);

        ArrayList<InfoFilm> list_film = new ArrayList<>();
        list_film.add(film3);
        list_film.add(film2);
        list_film.add(film);
        list_film.sort(InfoFilm::compareTo);


        for (InfoFilm f : films) {
            log.info("jdj", f.toString());
        }
        jsonreturn.append("{");
        if(films.size() == 100){
            jsonreturn.append("\"info\":\"Résultat limité à 100 films\", \n");
        }

        jsonreturn.append("\"resultat\":[\n");
        for (InfoFilm f: films) {
            jsonreturn.append(f.toString());
            if(films.indexOf(f) != films.size()-1)
                jsonreturn.append(",\n");
        }
        jsonreturn.append("]\n}");

        System.out.println(jsonreturn.toString());

        return jsonreturn.toString();
    }

    private String checkConditionAndReturnsSQL(String toCheck){

        String subRequest = null;

        toCheck = delStartBlanks(toCheck);

        if(toCheck.startsWith("TITRE")){
            log.info("info", "TITRE found");
            subRequest = "\tselect id_film\n" +
                    "\tfrom recherche_titre\n" +
                    "\twhere titre match ?";
            toCheck = toCheck.substring(6, toCheck.length());
            toCheck = delStartBlanks(toCheck);
            vars.add(toCheck);
            //subRequest = subRequest.concat(toCheck+"' ");

        }
        else {
            if(toCheck.startsWith("PAYS")){
                log.info("info", "PAYS found");

                subRequest = "\tselect id_film\n\tfrom films f\n\tinner join pays py\n\ton py.code=f.pays\n\twhere (f.pays like ? or py.nom like ? )";
                toCheck = toCheck.substring(5, toCheck.length());
                toCheck = delStartBlanks(toCheck);
                vars.add(toCheck);
                vars.add(toCheck);
                //subRequest = subRequest.concat(toCheck+"' or py.nom like '"+toCheck+"' )\n ");
            }
            else {
                if(toCheck.startsWith("EN")){
                    log.info("info", "EN found");

                    subRequest = "\tselect id_film\n\tfrom films f\n\twhere f.annee = ?";
                    toCheck = toCheck.substring(3, toCheck.length());
                    toCheck = delStartBlanks(toCheck);
                    vars.add(toCheck);
                    //subRequest = subRequest.concat(toCheck+" \n");
                }
                else {
                    if(toCheck.startsWith("AVANT")){
                        log.info("info", "AVANT found");

                        subRequest = "\tselect id_film\n\tfrom films f\n\twhere f.annee < ?";
                        toCheck = toCheck.substring(6, toCheck.length());
                        toCheck = delStartBlanks(toCheck);
                        vars.add(toCheck);
                        //subRequest = subRequest.concat(toCheck+" \n)\n");
                    }
                    else {
                        if(toCheck.startsWith("APRES")){
                            log.info("info", "APRES found");

                            subRequest = "\tselect id_film\n\tfrom films f\n\twhere f.annee > ?";
                            toCheck = toCheck.substring(6, toCheck.length());
                            toCheck = delStartBlanks(toCheck);
                            vars.add(toCheck);
                            //subRequest = subRequest.concat(toCheck+" \n");
                        }
                        else {
                            if(toCheck.startsWith("DE")){
                                log.info("info", "DE found");

                                subRequest = getDEandAVECSubrequest(toCheck, 'R');
                            }
                            else {
                                if(toCheck.startsWith("AVEC")){
                                    log.info("info", "AVEC found");

                                    subRequest = getDEandAVECSubrequest(toCheck, 'A');
                                }
                            }
                        }
                    }
                }
            }
        }




        return subRequest;
    }

    private String getDEandAVECSubrequest(String toCheck, char DEorAVEC){
        String subRequest = null;

            ArrayList<String> nom = new ArrayList<>();
            StringBuilder word = new StringBuilder();
            int i=0;

            if(DEorAVEC == 'R')
                toCheck = toCheck.substring(3, toCheck.length());
            else
                toCheck = toCheck.substring(5, toCheck.length());
        toCheck = delStartBlanks(toCheck);
            while(i < toCheck.length()){
                while( i < toCheck.length() && !Character.isWhitespace(toCheck.charAt(i)) ){
                    word.append(toCheck.charAt(i));
                    i++;
                }
                nom.add(word.toString());
                word.delete(0, word.length());
                i++;
            }
            switch (nom.size()) {

                case 1:
                    subRequest = "\tselect f.id_film\n" +
                            "\tfrom films f\n" +
                            "\tjoin generique g\n" +
                            "\ton g.id_film = f.id_film\n" +
                            "\tjoin personnes p\n" +
                            "\ton p.id_personne = g.id_personne\n" +
                            "\twhere g.role = '"+DEorAVEC+"'\n" +
                            "\t\tand (\n" +
                            "\t\t\tp.nom_sans_accent like ?"+
                            "\t\t)\n";
                    vars.add(nom.get(0));
                    break;

                case 2:
                    subRequest = "\tselect f.id_film\n" +
                            "\tfrom films f\n" +
                            "\tjoin generique g\n" +
                            "\ton g.id_film = f.id_film\n" +
                            "\tjoin personnes p\n" +
                            "\ton p.id_personne = g.id_personne\n" +
                            "\twhere g.role = '"+DEorAVEC+"'\n" +
                                "\t\tand (\n" +
                                    "\t\t\t(p.nom_sans_accent like ? and p.prenom_sans_accent like (? || '%'))\n" +
                                    "\t\t\tor\n" +
                                    "\t\t\t(p.nom_sans_accent like ? and p.prenom_sans_accent like (? || '%'))\n" +
                                "\t\t)\n";
                            vars.add(nom.get(0));
                        vars.add(nom.get(1));
                        vars.add(nom.get(1));
                            vars.add(nom.get(0));

                    break;

                case 3:
                    subRequest = "\tselect f.id_film\n" +
                            "\tfrom films f\n" +
                            "\tjoin generique g\n" +
                            "\ton g.id_film = f.id_film\n" +
                            "\tjoin personnes p\n" +
                            "\ton p.id_personne = g.id_personne\n" +
                            "\twhere g.role = '"+DEorAVEC+"'\n" +
                            "\t\tand (\n" +
                            "\t\t\t(\n" +
                            "\t\t\t\t(p.nom_sans_accent like ? and p.prenom_sans_accent like (? || '%'))\n" +
                                    "\t\t\t\tor\n" +
                                    "\t\t\t\t(p.nom_sans_accent like ? and p.prenom_sans_accent like (? || '%'))\n" +
                                    "\t\t\t)\n" +
                                    "\t\t\tor\n" +
                                    "\t\t\t(\n" +
                                    "\t\t\t\t(p.nom_sans_accent like ? and p.prenom_sans_accent like (? || '%'))\n" +
                                    "\t\t\t\tor\n" +
                                    "\t\t\t\t(p.nom_sans_accent like ? and p.prenom_sans_accent like (? || '%'))"+
                            "\t\t\t)\n" +
                        "\t\t)\n";
                    vars.add(nom.get(0).concat(" ").concat(nom.get(1)));
                    vars.add(nom.get(2));
                    vars.add(nom.get(2));
                    vars.add(nom.get(0).concat(" ").concat(nom.get(1)));
                    vars.add(nom.get(0));
                    vars.add(nom.get(1).concat(" ").concat(nom.get(2)));
                    vars.add(nom.get(1).concat(" ").concat(nom.get(2)));
                    vars.add(nom.get(0));
                    break;
            }


        return subRequest;
    }

    private String delStartBlanks(String toDel){

        while(toDel.charAt(0) == ' '){
            toDel = toDel.replaceFirst(" ", "");
        }

        return toDel;
    }


}

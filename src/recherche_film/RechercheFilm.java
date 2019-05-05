package recherche_film;

import logger.CompositeLogger;
import logger.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class RechercheFilm {

    private CompositeLogger log;
    private Connection JDBCConn;

    public static void main(String[] args) {
        try {

            RechercheFilm RF = new RechercheFilm("bdfilm.sqlite");
            RF.retrouve("EN 1999");
            RF.fermeBase();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    RechercheFilm(String nomFicherSQLite) throws IOException {

        log = LoggerFactory.getConsLogger("log.txt");

        JDBCConn = null;
        try {

            // db parameters
            String url = "jdbc:sqlite:"+nomFicherSQLite;
            log.info("JDBC", "JDBC URL : " + url);

            // create a connection to the database
            JDBCConn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
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

    public org.json.JSONObject retrouve(String RQS){
        org.json.JSONObject retour=null;
        String[] parts = null;

        StringBuilder request = new StringBuilder("with filtre as ( \n");
        String flatRequestPart = "select f.id_film, f.titre, a.titre, f.annee, py.nom, p.prenom, p.nom, g.role," +
                " group_concat(a.titre, '|') as autres_titres\n" +
                "from filtre\n" +
                "join films f\n" +
                "on f.id_film = filtre.id_film\n" +
                "join pays py\n" +
                "on py.code = f.pays\n" +
                "left join autres_titres a\n" +
                "on a.id_film = f.id_film\n" +
                "join generique g\n" +
                "on g.id_film = f.id_film\n" +
                "join personnes p\n" +
                "on p.id_personne = g.id_personne\n" +
                "group by f.id_film, f.titre, a.titre, f.annee, p.prenom, p.nom, g.role";

        //RQS = RQS.toLowerCase();
        parts = RQS.split(",");
        ArrayList<String> Parts = new ArrayList<>(Arrays.asList(parts));

        for(int i = 0; i<parts.length; i++){
            /*CheckConditions and add to request (check methods must return the string to add to the request)*/
            request.append(checkConditionAndReturnsSQL(Parts.get(i)));
            if((parts.length > 1) && i != parts.length - 1) request.append(" intersect ");
        }


        request.append(flatRequestPart);
        log.info("retrouve", "Request : \n"+request);

        try (Statement stmt  = JDBCConn.createStatement();
             ResultSet rs    = stmt.executeQuery(request.toString())){

            //InfoFilm film = new InfoFilm()
            //log.info("test", "Film 1 : "+rs.getString(1)+rs.getString(2)+rs.getString(3)+rs.getString(4)+rs.getString(5)+rs.getString(6)+rs.getString(7)+rs.getString(8)+rs.getString(9));


            // loop through the result set
            while (rs.next()) {
                System.out.println(rs.getString("titre") +  " "+rs.getString(5) + " "+rs.getString("annee"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }





        return retour;
    }

    private String checkConditionAndReturnsSQL(String toCheck){

        String subRequest = null;

        toCheck = delStartBlanks(toCheck);

        if(toCheck.startsWith("TITRE")){

            subRequest = "\tselect id_film\n" +
                    "\tfrom recherche_titre\n" +
                    "\twhere titre match '";
            toCheck = toCheck.substring(6, toCheck.length());

            toCheck = delStartBlanks(toCheck);
            subRequest = subRequest.concat(toCheck+"' \n) ");

        }
        else {
            if(toCheck.startsWith("PAYS")){
                subRequest = "\tselect id_film\n\tfrom films f\n\tinner join pays py\n\ton py.code = f.pays\n\twhere (f.pays like '";
                toCheck = toCheck.substring(5, toCheck.length());
                toCheck = delStartBlanks(toCheck);
                subRequest = subRequest.concat(toCheck+"' or py.nom like '"+toCheck+"' )\n)\n ");
            }
            else {
                if(toCheck.startsWith("EN")){
                    subRequest = "\tselect id_film\n\tfrom films f\n\twhere f.annee = ";
                    toCheck = toCheck.substring(3, toCheck.length());
                    toCheck = delStartBlanks(toCheck);
                    subRequest = subRequest.concat(toCheck+" \n)\n");
                }
                else {
                    if(toCheck.startsWith("AVANT")){
                        subRequest = "\tselect id_film\n\tfrom films f\n\twhere f.annee < ";
                        toCheck = toCheck.substring(6, toCheck.length());
                        toCheck = delStartBlanks(toCheck);
                        subRequest = subRequest.concat(toCheck+" \n)\n");
                    }
                    else {
                        if(toCheck.startsWith("APRES")){
                            subRequest = "\tselect id_film\n\tfrom films f\n\twhere f.annee > ";
                            toCheck = toCheck.substring(6, toCheck.length());
                            toCheck = delStartBlanks(toCheck);
                            subRequest = subRequest.concat(toCheck+" \n)\n");
                        }
                        else {
                            if(toCheck.startsWith("DE")){
                                int nameSize;
                                ArrayList<String> nom = new ArrayList<>();
                                StringBuilder word = new StringBuilder();
                                int i=0;
                                subRequest = "\tselect id_film\n" +
                                        "\tfrom films f\n" +
                                        "\tjoin generique g\n" +
                                        "\ton g.id_film = f.id_film\n" +
                                        "\tjoin personnes p\n" +
                                        "\ton p.id_personne = g.id_personne\n" +
                                        "\twhere g.role = 'R'\n" +
                                        "\t\tand (\n" +
                                        "\t\t\t(p.nom_sans_accent like '";
                                toCheck = toCheck.substring(3, toCheck.length());
                                toCheck = delStartBlanks(toCheck);
                                nameSize = toCheck.length();
                                while(i != nameSize-1){
                                    while(!Character.isWhitespace(toCheck.charAt(i))){
                                        word.append(toCheck.charAt(i));
                                        i++;
                                    }
                                    nom.add(word.toString());
                                    word.delete(0, word.length());
                                    i++;
                                }

                            }
                        }
                    }
                }
            }
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

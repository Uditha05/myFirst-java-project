package emailclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.Scanner;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

/*
 create main class to implement Email Client
 */
public class Email_Client1 {

    public static void main(String[] args) throws Exception {
        try {
            File file = new File("clientList.txt");    // create new text file to get new email recipient
            file.createNewFile();
            File file2 = new File("Receive Mail.txt");
            file2.createNewFile();
        } catch (Exception e) {
            System.out.println("'Error happen");
        }

        Addmin addmin = new Addmin();
        ObserverMail esp = new EmailStatPrinter();
        ObserverMail esr = new EmailStatRecorder();

        Thread e_Receiver = new EmailReceiver(addmin, esr, esp);
        Thread r_Saver = new ReceiveSaver(addmin);

        e_Receiver.start();
        r_Saver.start();

        ToDay today = new ToDay();
        String day = today.dategive();
        String fulldate = today.dateWithYear();
        System.out.println(day);

        addmin.getEmailsSent(day);

        System.out.println("Enter option type: \n"
                + "1 - Adding a new recipient\n"
                + "2 - Sending an email\n"
                + "3 - Printing out all the recipients who have birthdays\n"
                + "4 - Printing out details of all the emails sent\n"
                + "5 - Printing out the number of recipient objects in the application\n"
                + "6 - Terminate the program");

        loop1:
        while (true) {
            Scanner scanner = new Scanner(System.in);
            int option = Integer.parseInt(scanner.nextLine().trim());
            switch (option) {
                case 1:
                    System.out.println("input Recipient detail - Ex:- Official: nimal,nimal@gmail.com,ceo");
                    String inputRep = scanner.nextLine();
                    addmin.writeReptoFile(inputRep);
                    System.out.println("workdone !");
                    break;

                case 2:
                    System.out.println("input email details  -Ex:- email, subject, content");
                    String[] emailDetails = scanner.nextLine().trim().split(",");
                    addmin.sendMail(emailDetails);
                    break;

                case 3:
                    System.out.println(" input date - Ex:- yyyy/MM/dd (ex: 2018/09/17)");
                    String date1 = scanner.nextLine().trim();
                    addmin.getBirthDayRep(date1);
                    break;

                //Printing out details (subject and recipient) of all the emails sent on a date specified by user input            
                case 4:
                    System.out.println(" input current date - Ex:- yyyy/MM/dd (ex: 2018/09/17)");
                    String date2 = scanner.nextLine().trim();
                    System.out.println(date2);

                    addmin.readMail(date2);
                    break;

                case 5:
                    addmin.get_RepCount();
                    break;

                case 6:
                    e_Receiver.stop();
                    r_Saver.stop();
                    System.out.println("stop all threads");
                    break loop1;

            }
        }

    }

}

/*
 create addministator class to handle program
 all method hanle via this class
 all things access via this class
 */
class Addmin {

    private ArrayList<Recipient> rep_list;
    private ArrayList<Send_Email> email_list;
    private ArrayList<E_mail> receivemail;

    public Addmin() throws IOException, ClassNotFoundException {
        Loader loader = new Loader();
        this.rep_list = loader.getList();       // load previous recipient to appliction
        this.email_list = new ArrayList();
        this.receivemail = new ArrayList();
    }

    public void writeReptoFile(String st1) {
        try {
            //  upadting Recipient List 
            File file1 = new File("clientList.txt");
            BufferedWriter bw;
            bw = new BufferedWriter(new FileWriter(file1, true));
            bw.append(st1 + "\n");
            bw.close();
        } catch (IOException e) {
            System.out.println("Error occurr in writing Recipient !");
        }

    }

    public ArrayList<Recipient> getRepList() {
        return this.rep_list;
    }

    public void sendMail(String[] st) throws IOException {
        Send_Email sendEmail = new Send_Email(st);
        sendEmail.send();
        savingProcess(sendEmail);
    }

    public ArrayList<Send_Email> getmailList() {
        return this.email_list;
    }

    public void getBirthDayRep(String st3) {
        System.out.println("Birthay Recipients in " + st3);
        for (Recipient obj : this.rep_list) {
            if (obj instanceof Wishable) {
                if (obj.get_birthday().equals(st3)) {
                    System.out.println(obj.get_name());
                }
            }
        }
    }

    public void getEmailsSent(String date) throws IOException {
        if (this.rep_list == null) {
            return;
        }
        for (Recipient obj : this.rep_list) {
            if (obj instanceof Wishable) {
                if (obj.get_bDay().equals(date)) {
                    savingProcess(obj.sendBD());
                }
            }
        }
    }

    public void get_RepCount() {
        System.out.println("count of Recipient object : " + this.rep_list.size());
    }

    public void readMail(String date) throws IOException {
        SavingData saveing = new SavingData();
        this.email_list = saveing.importEmails();
        //                                                System.out.println(this.email_list.size());
        for (Send_Email ee : this.email_list) {
            //                                                  System.out.println(ee.getmaildate()  );
            if (ee.getmaildate().equals(date)) {
                //                                          System.out.println("pass this case");
                System.out.println(ee.getSub() + ee.gettingMail());
            }

        }

    }

    public void savingProcess(Send_Email se) throws IOException {
        ArrayList<Send_Email> allmail = new ArrayList<Send_Email>();
        SavingData savingdata = new SavingData();
        ArrayList<Send_Email> mail = savingdata.importEmails();
        allmail.addAll(mail);
        allmail.add(se);
        savingdata.emailSave(allmail);
    }

    public synchronized void putMail(E_mail mails) {
        this.receivemail.add(mails);
    }

    public synchronized ArrayList<E_mail> getMail() {
        ArrayList<E_mail> tempMail = new ArrayList<E_mail>();
        tempMail.addAll(this.receivemail);
        this.receivemail.clear();
        return tempMail;

    }

}

/*
 make abstract class to hold recipient object

 */
abstract class Recipient {

    protected String type;
    protected String name;
    protected String emailAdd;
    protected static int no_Obj = 0;

    public Recipient(String type, String name, String emailAdd) {
        this.type = type;
        this.name = name;
        this.emailAdd = emailAdd;
        no_Obj = no_Obj + 1;
    }

    public abstract String get_type();

    public abstract String get_name();

    public abstract String get_email();

    public abstract String get_bDay();

    public abstract String get_birthday();

    public abstract Send_Email sendBD();

    public static int get_Objcount() {
        return no_Obj;
    }

}

/*
 make Office Close Friend class to create  office close friend recipient


 */
class OfficeFriend extends Recipient implements Wishable {

    private String designation;
    private String birthday;

    public OfficeFriend(String type, String name, String emailAdd, String designation, String birthday) {

        super(type, name, emailAdd);
        this.designation = designation;
        this.birthday = birthday;

    }

    @Override
    public String get_name() {
        return name;
    }

    @Override
    public String get_birthday() {
        return birthday;
    }

    @Override
    public String get_bDay() {
        String day = this.birthday.substring(5);

        return day;
    }

    @Override
    public String get_email() {
        return emailAdd;
    }

    @Override
    public String get_type() {
        return type;
    }

    @Override
    public Send_Email sendBD() {
        String wish = "Wish you Happy Birthday. Uditha";
        BirthDay bd = new BirthDay(this.emailAdd, wish);
        return bd.sendwish();
    }

}

/*
 create class to get personal object
 */
class Personal extends Recipient implements Wishable {

    private String nickname;
    private String birthday;

    public Personal(String type, String name, String nickname, String emailAdd, String birthday) {
        super(type, name, emailAdd);
        this.nickname = nickname;
        this.birthday = birthday;
        no_Obj = no_Obj + 1;

    }

    @Override
    public String get_name() {
        return name;
    }

    public String get_birthday() {
        return birthday;
    }

    public String get_bDay() {
        String day = this.birthday.substring(5);
        return day;
    }

    public String get_nickname() {
        return nickname;
    }

    @Override
    public String get_email() {
        return emailAdd;
    }

    @Override
    public String get_type() {
        return type;
    }

    public Send_Email sendBD() {
        String wish = "Hugs and love on your birthday. Uditha";
        BirthDay bd = new BirthDay(this.emailAdd, wish);
        return bd.sendwish();
    }

}

/*
 create class to get Official non friends

 */
class NonOfficeRep extends Recipient {

    private final String designation;

    public NonOfficeRep(String type, String name, String emailAdd, String designation) {
        super(type, name, emailAdd);
        this.designation = designation;

        no_Obj = no_Obj + 1;
    }

    @Override
    public String get_name() {
        return name;
    }

    @Override
    public String get_email() {
        return emailAdd;
    }

    @Override
    public String get_type() {
        return type;
    }

    public String get_bDay() {
        return null;
    }

    public Send_Email sendBD() {
        return null;
    }

    @Override
    public String get_birthday() {
        return null;
    }

}

interface Wishable {

}

/*
 create class to get details list
 details list gives like == [type, name, nickname, email, designation , birthday]
 */
class FilterText {

    private final String details;
    private ArrayList<String> detailsList = new ArrayList<>();

    public FilterText(String details) {
        this.details = details;
    }

    public ArrayList<String> recip_Details() {
        String[] temp = this.details.split(":");
        String[] temp2 = temp[1].trim().split(",");
        String type = temp[0];

        if ("Personal".equals(type)) {

            detailsList.add(type);
            detailsList.add(temp2[0]);      // name 
            detailsList.add(temp2[1]);      // nick name
            detailsList.add(temp2[2]);      // Email 
            detailsList.add(temp2[3]);      //  BirthDay 

        } else if ("Official".equals(type)) {

            detailsList.add(type);
            detailsList.add(temp2[0]);      //  name 
            detailsList.add(temp2[1]);      // Email 
            detailsList.add(temp2[2]);      // Designation

        } else if ("Office_friend".equals(type)) {

            detailsList.add(type);
            detailsList.add(temp2[0]);      //  name 
            detailsList.add(temp2[1]);      // Email 
            detailsList.add(temp2[2]);      // Designation 
            detailsList.add(temp2[3]);      // birthday 

        } else {
            System.out.println(" invalid input ");
        }

        return this.detailsList;
    }

}

/*
 create class to sending emails and make email object
 */
class Send_Email implements Serializable {

    private String mailAdd;
    private String mailSubj;
    private String mailCont;
    private final String date;

    public Send_Email(String[] arry2) {
        this.mailAdd = arry2[0].trim();
        this.mailSubj = arry2[1].trim();
        this.mailCont = arry2[2].trim();
        ToDay d = new ToDay();
        this.date = d.dateWithYear();
    }

    public String gettingMail() {
        return this.mailAdd;
    }

    public String getSub() {
        return this.mailSubj;
    }

    public String getmaildate() {
        return this.date;
    }

    public void send() {
        //sample email and password...this are not valid 
        final String username = "email@gmail.com";
        final String password = "password";

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); //TLS

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("email@gmail.com"));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(this.mailAdd)
            );
            message.setSubject(this.mailSubj);
            message.setText(this.mailCont);

            Transport.send(message);

            System.out.println("Done");

        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }

}

/*
 create class to make birthday wishes
 */
class BirthDay {

    public String mailadd;
    public static String subject = "Birth Day Wish";
    public String wish;

    public BirthDay(String mailadd, String wish) {
        this.mailadd = mailadd;
        this.wish = wish;
    }

    public Send_Email sendwish() {

        String[] array = {mailadd, subject, wish};
        Send_Email sendmail1 = new Send_Email(array);
        sendmail1.send();
        return sendmail1;

    }

}
/*
 create class to save emails object  and load  previous mails
 */

class SavingData {

    public SavingData() throws IOException {
        File eMail = new File("EmailData.txt");
        if (!eMail.exists()) {
            eMail.createNewFile();
        }
    }

    public void emailSave(ArrayList<Send_Email> emaillist) {
        FileOutputStream fos = null;
        ObjectOutputStream output = null;
        try {
            fos = new FileOutputStream("EmailData.txt");
            output = new ObjectOutputStream(fos);
            output.writeObject(emaillist);
            if (fos != null) {
                fos.close();
            }
            if (output != null) {
                output.close();
            }

        } catch (IOException e) {

        }

    }

    public ArrayList<Send_Email> importEmails() {
        ArrayList<Send_Email> emails = null;
        FileInputStream iStream = null;
        ObjectInputStream obj_Of_emails = null;
        try {
            iStream = new FileInputStream("EmailData.txt");
            obj_Of_emails = new ObjectInputStream(iStream);

            emails = (ArrayList<Send_Email>) obj_Of_emails.readObject();
            if (iStream != null) {
                iStream.close();
            }
            if (obj_Of_emails != null) {
                obj_Of_emails.close();
            }
        } catch (IOException e) {

        } catch (ClassNotFoundException e) {
            emails = new ArrayList<Send_Email>();
        }
        if (emails == null) {
            return new ArrayList<Send_Email>();
        }
        return emails;
    }

}

/*
 create class to read text file and make recipient object
 return list of recipient object
 */
class Loader {

    private FilterText filter;
    private ArrayList<Recipient> temp = new ArrayList<>();

    public void loadAllRep() {

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("clientList.txt"));
            String line = reader.readLine();
            while (line != null) {
                writeRep(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error in read files");
        }

    }

    public void writeRep(String st1) {

        filter = new FilterText(st1);
        writeRepObj(filter.recip_Details());
    }

    public void writeRepObj(ArrayList arr1) {
        String type = arr1.get(0).toString();

        switch (type) {
            case "Personal":
                makePersonalObj(arr1);
                break;
            case "Official":
                makeOfficeObj(arr1);
                break;
            case "Office_friend":
                makeFriendObj(arr1);
                break;
        }

    }

    public void makePersonalObj(ArrayList arr2) {
        Recipient p_Obj = new Personal(arr2.get(0).toString(), arr2.get(1).toString(), arr2.get(2).toString(), arr2.get(3).toString(), arr2.get(4).toString());
        temp.add(p_Obj);
    }

    public void makeFriendObj(ArrayList arr2) {
        Recipient f_Obj = new OfficeFriend(arr2.get(0).toString(), arr2.get(1).toString(), arr2.get(2).toString(), arr2.get(3).toString(), arr2.get(4).toString());
        temp.add(f_Obj);
    }

    public void makeOfficeObj(ArrayList arr2) {
        Recipient o_Obj = new NonOfficeRep(arr2.get(0).toString(), arr2.get(1).toString(), arr2.get(2).toString(), arr2.get(3).toString());
        temp.add(o_Obj);
    }

    public ArrayList<Recipient> getList() {
        loadAllRep();
        return this.temp;
    }

}

/*
 create class to make current date 
 */
class ToDay {

    private String dateToday;
    Calendar calendar = Calendar.getInstance();

    public String dategive() {
        int x = calendar.get(Calendar.MONTH);
        int month = x + 1;
        dateToday = month + "/" + calendar.get(Calendar.DATE);

        return dateToday;
    }

    public String dateWithYear() {
        int x = calendar.get(Calendar.MONTH);
        int month = x + 1;
        dateToday = calendar.get(Calendar.YEAR) + "/" + month + "/" + calendar.get(Calendar.DATE);

        return dateToday;
    }
}

interface ObserverMail {

    public void update();
}

class E_mail implements Serializable {

    private String address;
    private String subject;
    private String date;
    private String content;

    public E_mail(String address, String subject, String content) {
        this.address = address;
        this.subject = subject;
        this.content = content;
        ToDay to_day = new ToDay();
        this.date = to_day.dategive();

    }

    public String getAddress() {
        return address;
    }

    public String getSubject() {
        return subject;
    }

    public String getDate() {
        return date;
    }

    public String getContent() {
        return content;
    }

}


/*
 #get mail from server and conver it to E_mail obj and put it in addmin Receivemail list 
 #notify all  observer
 */
class EmailReceiver extends Thread {

    private Addmin addmin;

    private static final String email_id = "email@gmail.com";
    private static final String password = "password";
    private Properties properties;
    private ArrayList<ObserverMail> obsMail;
    private boolean running;

    public EmailReceiver(Addmin addmin, ObserverMail esr, ObserverMail esp) {
        this.addmin = addmin;
        this.running = true;
        this.properties = new Properties();
        this.obsMail = new ArrayList<ObserverMail>();
        this.addObserver(esr);
        this.addObserver(esp);

        //necessary settings for IMAP protocol
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
    }

    @Override
    public void run() {

        while (running) {
            try {
                Session session = Session.getDefaultInstance(properties, null);

                Store store = session.getStore("imaps");

                store.connect(email_id, password);

                Folder inbox = store.getFolder("inbox");
                inbox.open(Folder.READ_WRITE);

                if (inbox.getUnreadMessageCount() > 0) {
                    int messageCount = inbox.getUnreadMessageCount();

                    Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                    int i = 0;
                    while (i < messageCount) {
                        Message message = messages[i];
                        E_mail email = new E_mail(message.getFrom()[0].toString(), message.getSubject(), getMessage(message));
                        this.addmin.putMail(email);

                        //notifying the observers
                        notifyall();
                        message.setFlags(new Flags(Flags.Flag.SEEN), true);
                        i++;
                    }
                } else {

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                    }
                }

                inbox.close(true);
                store.close();

            } catch (MessagingException e) {
                e.printStackTrace();
            }

        }
    }
// get the massage form Recevied email 

    private String getMessage(Message message) {
        String out = "";
        try {

            if (message.isMimeType("text/plain")) {
                out = message.getContent().toString();
            } else if (message.isMimeType("multipart/*")) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                out = getMimeMultipart(mimeMultipart);
            }

        } catch (MessagingException e) {

        } catch (IOException e) {

        }
        return out;
    }

    // get massage from MimeMultipart 
    private String getMimeMultipart(MimeMultipart mimeMultipart) {
        String out = "";
        try {
            int count = mimeMultipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    out = out + "\n" + bodyPart.getContent();
                    break;
                } else if (bodyPart.getContent() instanceof MimeMultipart) {
                    out = out + getMimeMultipart((MimeMultipart) bodyPart.getContent());
                }
            }
        } catch (MessagingException e) {

        } catch (IOException e) {

        }
        return out;
    }

    // method for notify all observers 
    public void notifyall() {
        for (ObserverMail OM : this.obsMail) {
            OM.update();
        }

    }

    public void addObserver(ObserverMail om1) {
        this.obsMail.add(om1);
    }

    public void removeObs(ObserverMail om2) {
        this.obsMail.remove(om2);
    }

}

/*
 save emails from addmin object .this is a thread 
 */
class ReceiveSaver extends Thread {

    private Addmin addmin;

    public ReceiveSaver(Addmin addmin) {
        this.addmin = addmin;
    }

    @Override
    public void run() {

        while (true) {
            ArrayList<E_mail> Email1 = addmin.getMail();
            if (!Email1.isEmpty()) {

                for (int i = 0; i < Email1.size(); i++) {
                    try {
                        saveReci_mail(Email1.get(i));
                        System.out.println("save complete");
                    } catch (IOException ex) {
                        System.out.println("Error in saving Recive mail process");
                    }
                }

            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                }

            }
        }
    }

    public void saveReci_mail(E_mail em) throws IOException {
        File ReciMail = new File("EmailReceiveData.txt");
        if (!ReciMail.exists()) {
            ReciMail.createNewFile();
        }

        FileOutputStream fos = null;
        ObjectOutputStream output = null;
        try {
            fos = new FileOutputStream("EmailReceiveData.txt");
            output = new ObjectOutputStream(fos);
            output.writeObject(em);
            if (fos != null) {
                fos.close();
            }
            if (output != null) {
                output.close();
            }

        } catch (IOException e) {
            System.out.println("error in e_mail object writing");
        }
    }

}


/*
 prints the same message("an email is received at <current time>".) to a text file in the hard disk. 

 */
class EmailStatPrinter implements ObserverMail {

    private String massage;

    public EmailStatPrinter() {
        this.massage = "an email is received at";
    }

    @Override
    public void update() {

        writeToFile();
    }

    public void writeToFile() {
        String new_msg = this.massage + " " + Calendar.getInstance().getTime().toString();

        try {
            //  upadting Receive Mail
            File file2 = new File("Receive Mail.txt");
            BufferedWriter bw;
            bw = new BufferedWriter(new FileWriter(file2, true));
            bw.append(new_msg + "\n");
            bw.close();
        } catch (IOException e) {
            System.out.println("Error occurr in writing Recipient !");
        }
    }
}


/*
 prints the following message to the console "an email is received at <current time>".
 */
class EmailStatRecorder implements ObserverMail {

    private String massage;

    public EmailStatRecorder() {
        this.massage = "an email is received at";
    }

    @Override
    public void update() {
//        String time = Calendar.getInstance().getTime().toString();
        System.out.println(massage + " "
                + Calendar.getInstance().getTime().toString());
    }
}

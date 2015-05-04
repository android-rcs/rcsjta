
package com.sonymobile.rcs.imap.tests;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JamesUtil {

    public static void createUser(String username, String password, boolean deleteIfExists)
            throws Exception {
        JMXServiceURL url = new JMXServiceURL(
                "service:jmx:rmi://localhost/jndi/rmi://localhost:9999/jmxrmi");
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        ObjectName usersrepository = new ObjectName(
                "org.apache.james:type=component,name=usersrepository");
        ObjectName domainlist = new ObjectName("org.apache.james:type=component,name=domainlist");

        String domain = username.substring(username.indexOf('@') + 1);

        boolean domainExists = (Boolean) mbsc.invoke(domainlist, "containsDomain", new Object[] {
            domain
        }, new String[] {
            String.class.getName()
        });

        if (!domainExists) {
            System.out.println("James: adding domain " + domain);
            mbsc.invoke(domainlist, "addDomain", new Object[] {
                domain
            }, new String[] {
                String.class.getName()
            });
        }

        boolean userExists = (Boolean) mbsc.invoke(usersrepository, "verifyExists", new Object[] {
            username
        }, new String[] {
            String.class.getName()
        });

        if (userExists && deleteIfExists) {

            System.out.println("James: deleting user " + username);
            mbsc.invoke(usersrepository, "deleteUser", new Object[] {
                username
            }, new String[] {
                String.class.getName()
            });
            userExists = false;

        }

        if (!userExists) {

            System.out.println("James: adding user " + username);
            mbsc.invoke(usersrepository, "addUser", new Object[] {
                    username, password
            }, new String[] {
                    String.class.getName(), String.class.getName()
            });

        } else {
            System.out.println("James: user already exists " + username);

        }

        jmxc.close();
    }

}

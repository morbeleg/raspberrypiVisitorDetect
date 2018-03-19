package knockApi.dao;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DaoHandler {
    private static SessionFactory factory;

    public static SessionFactory getLazyFactory() {
        try {
            if (factory == null)
                return factory = new Configuration().configure().buildSessionFactory();
            else
                return factory;
        } catch (Throwable ex) {
            System.err.println("Failed to create sessionFactory object." + ex);
            if (!factory.isClosed()) {
                factory.close();
            }
            throw new ExceptionInInitializerError(ex);
        }
    }


}

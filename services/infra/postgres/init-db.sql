-- commons, only for integration tests purposes
CREATE USER commons WITH ENCRYPTED PASSWORD 'commons';
CREATE DATABASE commons;
GRANT ALL PRIVILEGES ON DATABASE commons TO commons;
ALTER DATABASE commons OWNER TO commons; -- needed since postgres v15

-- ordering
CREATE USER ordering WITH ENCRYPTED PASSWORD 'ordering';
CREATE DATABASE ordering;
GRANT ALL PRIVILEGES ON DATABASE ordering TO ordering;
ALTER DATABASE ordering OWNER TO ordering;

-- delivery
CREATE USER delivery WITH ENCRYPTED PASSWORD 'delivery';
CREATE DATABASE delivery;
GRANT ALL PRIVILEGES ON DATABASE delivery TO delivery;
ALTER DATABASE delivery OWNER TO delivery;

-- food
CREATE USER food WITH ENCRYPTED PASSWORD 'food';
CREATE DATABASE food;
GRANT ALL PRIVILEGES ON DATABASE food TO food;
ALTER DATABASE food OWNER TO food;

-- bff
CREATE USER bff WITH ENCRYPTED PASSWORD 'bff';
CREATE DATABASE bff;
GRANT ALL PRIVILEGES ON DATABASE bff TO bff;
ALTER DATABASE bff OWNER TO bff;
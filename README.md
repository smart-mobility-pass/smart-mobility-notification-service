# Smart Mobility - Notification Service 📧📱

Le **Notification Service** est un microservice critique de la plateforme *Smart Mobility*. Son rôle unique (principe de responsabilité unique) est de consommer des événements asynchrones métiers (Paiements, Rechargements de portefeuille) et d'alerter les utilisateurs finaux via différents canaux (e-mail, SMS, Push). 

Il ne contient **aucune logique de calcul financier ou de facturation**. Il réagit aux événements et informe.

## 🎯 Architecture et Principes

Ce service a été pensé pour être **découplé, résilient et idempotent** :

*   **Architecture orientée événements (Event-Driven) :** Il est déconnecté des autres microservices (Billing, Pricing). Il écoute passivement les files RabbitMQ.
*   **Idempotence :** Pour éviter d'envoyer deux fois la même notification (ex: lors d'une reconnexion réseau ou d'un *retry* de RabbitMQ), chaque événement est tracé à l'aide d'une `idempotencyKey` unique avant tout traitement.
*   **Traçabilité Auditable :** Avant l'envoi, l'intention de notification est sauvegardée (Statut `PENDING`). Après l'envoi, elle est mise à jour (`SENT` ou `FAILED`). Cela permet un audit parfait et un système de re-jeu (*Replay*) en cas de panne du fournisseur d'e-mail.
*   **Tolérance aux Pannes :** En cas d'erreur fatale, le message n'est pas perdu et finit dans une File de Lettres Mortes (Dead Letter Queue - DLQ) pour investigation.

## ⚙️ Stack Technique

*   **Framework :** Spring Boot 3.x
*   **Langage :** Java 17+
*   **Base de données :** MySQL (JPA / Hibernate)
*   **Messaging :** RabbitMQ (Spring AMQP)
*   **Client HTTP :** OpenFeign (Communication avec *User Service*)
*   **Sécurité :** Spring Security, OAuth2 Resource Server (JWT via Keycloak)
*   **Observabilité :** Actuator, Micrometer (Zipkin & Brave)
*   **Mailing :** Spring Boot Mail (JavaMailSender)

---

## 🐇 Evénements et Messaging (RabbitMQ)

Le Notification Service s'abonne à des **Topics** spécifiques.

### 1. Payment Events (Paiements)
Générés par le `Billing Service` lors du dénouement d'un trajet.
*   **Exchange :** `payment.exchange`
*   **Routing Keys :** `payment.completed`, `payment.failed`
*   **Queue du Service :** `notification.payment.queue`

Payload (Modèle) :
```json
{
  "tripId": "12345ABC",
  "userId": 42,
  "amount": 550.00,
  "status": "SUCCESS", // ou FAILED
  "reason": null,
  "timestamp": "2026-02-25T15:30:00Z"
}
```

### 2. Account Credited Events (Rechargements)
Générés lors d'une recharge de portefeuille (Top-Up).
*   **Exchange :** `account.exchange`
*   **Routing Key :** `account.credited`
*   **Queue du Service :** `notification.account.queue`

Payload (Modèle) :
```json
{
  "userId": 42,
  "amount": 10000.00,
  "timestamp": "2026-02-25T16:00:00Z"
}
```

---

## 🗄️ Modèle de Données (Base de données)

L'entité centrale est `Notification`. La table générée stocke en toute sécurité le cycle de vie de l'envoi.

| Colonne | Type | Description |
| :--- | :--- | :--- |
| `id` | PK (Long) | Identifiant unique de la transaction |
| `user_id` | Long | L'utilisateur destinataire (référencé dans le *User Service*) |
| `reference_id` | String | L'ID de l'événement déclencheur (ex: `tripId`) |
| `reference_type` | Enum | Origine (`TRIP`, `TOP_UP`, `SUBSCRIPTION`, `OTHER`) |
| `notification_type` | Enum | Nature (`PAYMENT`, `ACCOUNT_CREDITED`, `LOW_BALANCE`, `INFO`) |
| `channel` | Enum | Canal utilisé (`EMAIL`, `SMS`, `PUSH`) |
| `message` | Text | Le contenu formaté du message envoyé |
| `status` | Enum | État de l'envoi (`PENDING`, `SENT`, `FAILED`) |
| `idempotency_key` | String (Unique) | Clé de dé-duplication pour garantir l'unicité du traitement |
| `created_at` | Timestamp | Date de réception de l'événement |

---

## 🛠️ Configuration (Variables d'Environnement)

Pour lancer le service, les variables d'environnement suivantes doivent ou peuvent être définies (cf. `application.yml`) :

```env
# Configuration Globale
SERVER_PORT=8086
EUREKA_HOST=localhost

# Base de Données
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=root

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# Keycloak (Sécurité JWT)
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/smart-mobility

# Configuration Serveur Mail (SMTP)
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
```

---

## 🚀 Lancement Rapide (Mode Local)

1. Assurez-vous que l'infrastructure sous-jacente tourne via Docker (RabbitMQ, MySQL, Keycloak, Eureka). Si vous voulez tester les emails facilement, lancez un conteneur **MailHog** (ou utilisez MailTrap).
2. Lancez l'application :
   ```bash
   ./mvnw spring-boot:run
   ```
3. L'application devrait s'enregistrer auprès d'Eureka Client et commencer à écouter passivement les files RabbitMQ. Si des événements `Payment` sont déclenchés ailleurs dans le système, vous les verrez traités dans les logs et les envois de mail interceptés.

---

## 🔮 Évolutions Futures (V2)

Bien que prêt pour la production, le service est conçu pour accueillir les évolutions suivantes :
1. **Templates HTML :** Intégration de Thymeleaf pour envoyer des e-mails esthétiquement riches (au lieu du format texte brut utilisé dans la V1).
2. **Nouveaux Canaux :** Implémentation de Twilio (pour les SMS) ou Firebase Cloud Messaging (pour les Push notifications mobiles), gérés dynamiquement grâce à l'Enum `NotificationChannel`.
3. **Préférences Utilisateurs :** Appel conditionnel au endpoint Feign pour récupérer les choix de l'utilisateur (ex: "Accepte les Publicités", "Préfère SMS", etc.).
4. **Retry Asynchrone :** Mise en place d'un *@Scheduled* job pour relancer périodiquement les notifications bloquées à l'état `FAILED`.

package OBM::Entities::obmUser;

$VERSION = "1.0";

$debug = 1;

use 5.006_001;
require Exporter;
use strict;

use OBM::Parameters::common;
use OBM::Parameters::ldapConf;
require OBM::Ldap::utils;
require OBM::passwd;
require OBM::toolBox;
require OBM::dbUtils;
use URI::Escape;
use Unicode::MapUTF8 qw(to_utf8 from_utf8 utf8_supported_charset);


sub new {
    my $self = shift;
    my( $incremental, $userId ) = @_;

    my %ldapEngineAttr = (
        type => undef,
        typeDesc => undef,
        incremental => undef,
        toDelete => undef,
        archive => undef,
        userId => undef,
        domainId => undef,
        userDesc => undef
    );


    if( !defined($userId) ) {
        croak( "Usage: PACKAGE->new(INCR, USERID)" );

    }elsif( $userId !~ /^\d+$/ ) {
        &OBM::toolBox::write_log( "obmUser: identifiant d'utilisateur incorrect", "W" );
        return undef;

    }else {
        $ldapEngineAttr{"userId"} = $userId;
    }

    if( $incremental ) {
        $ldapEngineAttr{"incremental"} = 1;
    }else {
        $ldapEngineAttr{"incremental"} = 0;
    }

    $ldapEngineAttr{"type"} = $POSIXUSERS;
    $ldapEngineAttr{"typeDesc"} = $attributeDef->{$ldapEngineAttr{"type"}};
    $ldapEngineAttr{"toDelete"} = 0;

    bless( \%ldapEngineAttr, $self );
}


sub getEntity {
    my $self = shift;
    my( $dbHandler, $domainDesc ) = @_;
    my $userId = $self->{"userId"};


    if( !defined($dbHandler) ) {
        &OBM::toolBox::write_log( "obmUser: connecteur a la base de donnee invalide", "W" );
        return 0;
    }

    if( !defined($domainDesc->{"domain_id"}) || ($domainDesc->{"domain_id"} !~ /^\d+$/) ) {
        &OBM::toolBox::write_log( "obmUser: description de domaine OBM incorrecte", "W" );
        return 0;

    }else {
        # On positionne l'identifiant du domaine de l'entité
        $self->{"domainId"} = $domainDesc->{"domain_id"};
    }


    my $query = "SELECT COUNT(*) FROM ".&OBM::dbUtils::getTableName("UserObm", $self->isIncremental())." LEFT JOIN ".&OBM::dbUtils::getTableName("MailServer", $self->isIncremental())." ON userobm_mail_server_id=mailserver_id WHERE userobm_id=".$userId;

    my $queryResult;
    if( !&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult ) ) {
        &OBM::toolBox::write_log( "obmUser: probleme lors de l'execution d'une requete SQL : ".$dbHandler->err, "W" );
        return undef;
    }

    my( $numRows ) = $queryResult->fetchrow_array();
    $queryResult->finish();

    if( $numRows == 0 ) {
        &OBM::toolBox::write_log( "obmUser: pas d'utilisateur d'identifiant : ".$userId, "W" );
        return undef;
    }elsif( $numRows > 1 ) {
        &OBM::toolBox::write_log( "obmUser: plusieurs utilisateurs d'identifiant : ".$userId." ???", "W" );
        return undef;
    }


    # La requete a executer - obtention des informations sur l'utilisateur
    $query = "SELECT userobm_id, userobm_archive, userobm_perms, userobm_login, userobm_password_type, userobm_password, userobm_uid, userobm_gid, userobm_lastname, userobm_firstname, userobm_address1, userobm_address2, userobm_address3, userobm_zipcode, userobm_town, userobm_title, userobm_service, userobm_description, userobm_mail_perms, userobm_mail_ext_perms, userobm_email, mailserver_host_id, userobm_mail_quota, userobm_vacation_enable, userobm_vacation_message, userobm_nomade_perms, userobm_nomade_enable, userobm_nomade_local_copy, userobm_email_nomade, userobm_web_perms, userobm_phone, userobm_phone2, userobm_fax, userobm_fax2, userobm_mobile FROM ".&OBM::dbUtils::getTableName("UserObm", $self->isIncremental())." LEFT JOIN ".&OBM::dbUtils::getTableName("MailServer", $self->isIncremental())." ON userobm_mail_server_id=mailserver_id WHERE userobm_id=".$userId;

    # On execute la requete
    if( !&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult ) ) {
        &OBM::toolBox::write_log( "obmUser: probleme lors de l'execution d'une requete SQL : ".$dbHandler->err, "W" );
        return undef;
    }

    # On range les resultats dans la structure de donnees des resultats
    my( $user_id, $user_archive, $user_perms, $user_login, $user_passwd_type, $user_passwd, $user_uid, $user_gid, $user_lastname, $user_firstname, $user_address1, $user_address2, $user_address3, $user_zipcode, $user_town, $user_title, $user_service, $user_description, $user_mail_perms, $user_mail_ext_perms, $user_email, $user_mail_server_id, $user_mail_quota, $user_vacation_enable, $user_vacation_message, $user_nomade_perms, $user_nomade_enable, $user_nomade_local_copy, $user_nomade_email, $user_web_perms, $user_phone, $user_phone2, $user_fax, $user_fax2, $user_mobile ) = $queryResult->fetchrow_array();
    $queryResult->finish();

    # Positionnement du flag archive
    $self->{"archive"} = $user_archive;
    if( $user_archive ) {
        &OBM::toolBox::write_log( "obmUser: gestion de l'utilisateur archive '".$user_login."', domaine '".$domainDesc->{"domain_label"}."'", "W" );

    }else {
        &OBM::toolBox::write_log( "obmUser: gestion de l'utilisateur '".$user_login."', domaine '".$domainDesc->{"domain_label"}."'", "W" );

    }

    # Gestion de l'UID
    my $user_real_uid = $user_uid;
    if( lc($user_perms) eq "admin" ) {
        $user_real_uid = 0;
    }
        
    # On cree la structure correspondante a l'utilisateur
    # Cette structure est composee des valeurs recuperees dans la base
    $self->{"userDesc"} = {
        "user_id"=>$user_id,
        "user_login"=>$user_login,
        "user_uid"=>$user_real_uid,
        "user_gid"=>$user_gid,
        "user_lastname"=>$user_lastname,
        "user_firstname"=>$user_firstname,
        "user_homedir"=>"$baseHomeDir/$user_login",
        "user_mailperms"=>$user_mail_perms,
        "user_webperms"=>$user_web_perms,
        "user_passwd_type"=>$user_passwd_type,
        "user_passwd"=>$user_passwd,
        "user_title"=>$user_title,
        "user_service"=>$user_service,
        "user_description"=>$user_description,
        "user_zipcode"=>$user_zipcode,
        "user_town"=>$user_town,
        "user_mobile"=>$user_mobile,
        "user_domain" => $domainDesc->{"domain_label"}
    };

    # gestion de l'adresse
    if( defined($user_address1) && ($user_address1 ne "") ) {
        $self->{"userDesc"}->{"user_address"} = $user_address1;
    }
        
    if( defined($user_address2) && ($user_address2 ne "") ) {
        $self->{"userDesc"}->{"user_address"} .= "\r\n".$user_address2;
    }
        
    if( defined($user_address3) && ($user_address3 ne "") ) {
        $self->{"userDesc"}->{"user_address"} .= "\r\n".$user_address3;
    }
        

    # Gestion du téléphone
    if( defined($user_phone) && ($user_phone ne "") ) {
        push( @{$self->{"userDesc"}->{"user_phone"}}, $user_phone );
    }

    if( defined($user_phone2) && ($user_phone2 ne "") ) {
        push( @{$self->{"userDesc"}->{"user_phone"}}, $user_phone2 );
    }

    # Gestion du fax
    if( defined($user_fax) && ($user_fax ne "") ) {
        push( @{$self->{"userDesc"}->{"user_fax"}}, $user_fax );
    }

    if( defined($user_fax2) && ($user_fax2 ne "") ) {
        push( @{$self->{"userDesc"}->{"user_fax"}}, $user_fax2 );
    }

    # Gestion du droit de messagerie
    if( $user_mail_perms ) {
        my $localServerIp = &OBM::toolBox::getHostIpById( $dbHandler, $user_mail_server_id );

        if( !defined($localServerIp) ) {
            &OBM::toolBox::write_log( "obmUser: droit mail de l'utilisateur '".$user_login."' annule - Serveur inconnu !", "W" );
            $self->{"userDesc"}->{"user_mailperms"} = 0;

        }else {
            $self->{"userDesc"}->{"user_mailperms"} = 1;

            # Limite la messagerie aux domaines locaux
            if( !$user_mail_ext_perms ) {
                $self->{"userDesc"}->{"user_mailLocalOnly"} = "local_only";
            }

            # Gestions des e-mails de l'utilisateur.
            my @email = split( /\r\n/, $user_email );
            for( my $j=0; $j<=$#email; $j++ ) {
                push( @{$self->{"userDesc"}->{"user_email"}}, $email[$j]."@".$domainDesc->{"domain_name"} );

                for( my $k=0; $k<=$#{$domainDesc->{"domain_alias"}}; $k++ ) {
                    push( @{$self->{"userDesc"}->{"user_email_alias"}}, $email[$j]."@".$domainDesc->{"domain_alias"}->[$k] );
                }
            }

            # Gestion des BAL destination
            $self->{"userDesc"}->{"user_mailbox"} = $self->{"userDesc"}->{"user_login"}."@".$domainDesc->{"domain_name"};

            # Gestion du serveur de mail
            $self->{"userDesc"}->{"user_mailbox_server"} = $user_mail_server_id;

            # Gestion du quota
            $self->{"userDesc"}->{"user_mailbox_quota"} = $user_mail_quota*1000;

            # Gestion du message d'absence
            $self->{"userDesc"}->{"user_vacation_enable"} = $user_vacation_enable;
            $self->{"userDesc"}->{"user_vacation_message"} = uri_unescape($user_vacation_message);

            # Gestion de la redirection d'adresse
            $self->{"userDesc"}->{"user_nomade_perms"} = $user_nomade_perms;
            $self->{"userDesc"}->{"user_nomade_enable"} = $user_nomade_enable;
            $self->{"userDesc"}->{"user_nomade_local_copy"} = $user_nomade_local_copy;
            $self->{"userDesc"}->{"user_nomade_email"} = $user_nomade_email;

            # Gestion de la livraison du courrier
            $self->{"userDesc"}->{"user_mailLocalServer"} = "lmtp:".$localServerIp.":24";
        }
    }

    # Si nous ne sommes pas en mode incrémental, on charge aussi les liens de
    # cette entité
    if( !$self->isIncremental() ) {
        $self->getEntityLinks( $dbHandler, $domainDesc );
    }


    return 1;
}


sub setDelete {
    my $self = shift;

    $self->{"toDelete"} = 1;

    return 1;
}


sub getDelete {
    my $self = shift;

    return $self->{"toDelete"};
}


sub getArchive {
    my $self = shift;

    return $self->{"archive"};
}


sub isIncremental {
    my $self = shift;

    return $self->{"incremental"};
}


sub getEntityLinks {
    my $self = shift;
    my( $dbHandler, $domainDesc ) = @_;

    $self->_getEntityMailboxAcl( $dbHandler, $domainDesc );

    # Du moment qu'on charge les liens de l'entité, cette entité n'est plus en
    # mode incrémental
    $self->{"incremental"} = 0;

    return 1;
}


sub _getEntityMailboxAcl {
    my $self = shift;
    my( $dbHandler, $domainDesc ) = @_;
    my $userId = $self->{"userId"};

    if( !$self->{"userDesc"}->{"user_mailperms"} ) {
        $self->{"userDesc"}->{"user_mailbox_acl"} = undef;
    }else {

        my $entityType = "mailbox";
        my %rightDef;

        $rightDef{"read"}->{"compute"} = 1;
        $rightDef{"read"}->{"sqlQuery"} = "SELECT i.userobm_login FROM ".&OBM::dbUtils::getTableName("UserObm", $self->isIncremental())." i, ".&OBM::dbUtils::getTableName("EntityRight", $self->isIncremental())." j WHERE i.userobm_id=j.entityright_consumer_id AND j.entityright_write=0 AND j.entityright_read=1 AND j.entityright_entity_id=".$userId." AND j.entityright_entity='".$entityType."'";

        $rightDef{"writeonly"}->{"compute"} = 1;
        $rightDef{"writeonly"}->{"sqlQuery"} = "SELECT i.userobm_login FROM ".&OBM::dbUtils::getTableName("UserObm", $self->isIncremental())." i, ".&OBM::dbUtils::getTableName("EntityRight", $self->isIncremental())." j WHERE i.userobm_id=j.entityright_consumer_id AND j.entityright_write=1 AND j.entityright_read=0 AND j.entityright_entity_id=".$userId." AND j.entityright_entity='".$entityType."'";

        $rightDef{"write"}->{"compute"} = 1;
        $rightDef{"write"}->{"sqlQuery"} = "SELECT userobm_login FROM ".&OBM::dbUtils::getTableName("UserObm", $self->isIncremental())." LEFT JOIN ".&OBM::dbUtils::getTableName("EntityRight", $self->isIncremental())." ON entityright_write=1 AND entityright_read=1 AND entityright_consumer_id=userobm_id AND entityright_entity='".$entityType."' WHERE entityright_entity_id=".$userId." OR userobm_id=".$userId;

        $rightDef{"public"}->{"compute"} = 0;
        $rightDef{"public"}->{"sqlQuery"} = "SELECT entityright_read, entityright_write FROM ".&OBM::dbUtils::getTableName("EntityRight", $self->isIncremental())." WHERE entityright_entity_id=".$userId." AND entityright_entity='".$entityType."' AND entityright_consumer_id=0";

        # On recupere la definition des ACL
        $self->{"userDesc"}->{"user_mailbox_acl"} = &OBM::toolBox::getEntityRight( $dbHandler, $domainDesc, \%rightDef, $userId );
    }

    return 1;
}


sub getLdapDnPrefix {
    my $self = shift;
    my $dnPrefix = undef;

    if( defined($self->{"typeDesc"}->{"dn_prefix"}) && defined($self->{"userDesc"}->{$self->{"typeDesc"}->{"dn_value"}}) ) {
        $dnPrefix = $self->{"typeDesc"}->{"dn_prefix"}."=".$self->{"userDesc"}->{$self->{"typeDesc"}->{"dn_value"}};
    }

    return $dnPrefix;
}


sub createLdapEntry {
    my $self = shift;
    my ( $ldapEntry ) = @_;
    my $entry = $self->{"userDesc"};

    #
    # Gestion du mot de passe
    if( !defined( $entry->{"user_passwd_type"} ) || ($entry->{"user_passwd_type"} eq "") ) {
        return 0;
    }

    my $userPasswd = &OBM::passwd::convertPasswd( $entry->{"user_passwd_type"}, $entry->{"user_passwd"} );
    if( !defined( $userPasswd ) ) {
        return 0;
    }


    #
    # On construit la nouvelle entree
    #
    # Les parametres nécessaires
    if( $entry->{"user_login"} && $entry->{"user_lastname"} && defined($entry->{"user_uid"}) && ($entry->{"user_uid"} ne "") && defined($entry->{"user_gid"})  && $entry->{"user_homedir"} ) {

        # Creation de la valeur du champs CN
        my $longName;
        if( $entry->{"user_firstname"} ) {
            $longName = $entry->{"user_firstname"}." ".$entry->{"user_lastname"};
        }else {
            $longName = $entry->{"user_lastname"};
        }
                
        $ldapEntry->add(
            objectClass => $self->{"typeDesc"}->{"objectclass"},
            uid => to_utf8({ -string => $entry->{"user_login"}, -charset => $defaultCharSet }),
            cn => to_utf8({ -string => $longName, -charset => $defaultCharSet }),
            sn => to_utf8({ -string => $entry->{"user_lastname"}, -charset => $defaultCharSet }),
            displayName => to_utf8({ -string => $longName, -charset => $defaultCharSet }),
            uidNumber => $entry->{"user_uid"},
            gidNumber => $entry->{"user_gid"},
            homeDirectory => $entry->{"user_homedir"},
            loginShell => "/bin/bash"
        );

    }else {
        return 0;
    }

    # Le prenom
    if( $entry->{"user_firstname"} ) {
        $ldapEntry->add( givenName => to_utf8({ -string => $entry->{"user_firstname"}, -charset => $defaultCharSet }) );
    }

    # Le mot de passe
    if( $userPasswd ) {
        $ldapEntry->add( userPassword => $userPasswd );
    }

    # Le telephone
    if( $entry->{"user_phone"} ) {
        $ldapEntry->add( telephoneNumber => $entry->{"user_phone"} );
    }

    # Le fax
    if( $entry->{"user_fax"} ) {
        $ldapEntry->add( facsimileTelephoneNumber => $entry->{"user_fax"} );
    }

    # Le mobile
    if( $entry->{"user_mobile"} ) {
        $ldapEntry->add( mobile => $entry->{"user_mobile"} );
    }

    # Le titre
    if( $entry->{"user_title"} ) {
        $ldapEntry->add( title => to_utf8({ -string => $entry->{"user_title"}, -charset => $defaultCharSet }) );
    }

    # Le service
    if( $entry->{"user_service"} ) {
        $ldapEntry->add( ou => to_utf8({ -string => $entry->{"user_service"}, -charset => $defaultCharSet }) );
    }

    # La description
    if( $entry->{"user_description"} ) {
        $ldapEntry->add( description => to_utf8({ -string => $entry->{"user_description"}, -charset => $defaultCharSet }) );
    }

    # L'acces web
    if( $entry->{"user_webperms"} || ( defined( $entry->{"user_webperms"} ) && ($entry->{"user_webperms"} == 0) ) ) {
        $ldapEntry->add( webAccess => $entry->{"user_webperms"} );
    }

    # La boite a lettres de l'utilisateur
    if( $entry->{"user_mailbox"} ) {
        $ldapEntry->add( mailBox => $entry->{"user_mailbox"} );
    }

    # Le serveur de BAL local
    if( $entry->{"user_mailLocalServer"} ) {
        $ldapEntry->add( mailBoxServer => $entry->{"user_mailLocalServer"} );
    }

    # L'acces mail
    if( $entry->{"user_mailperms"} ) {
        $ldapEntry->add( mailAccess => "PERMIT" );
    }else {
        $ldapEntry->add( mailAccess => "REJECT" );
    }

    # La limite aux domaines locaux
    if( $entry->{"user_mailLocalOnly"} ) {
        $ldapEntry->add( mailLocalOnly => $entry->{"user_mailLocalOnly"} );
    }

    # Les adresses mails
    if( $entry->{"user_email"} && ($#{$entry->{"user_email"}} != -1) ) {
        $ldapEntry->add( mail => $entry->{"user_email"} );
    }

    # Les adresses mail secondaires
    if( $entry->{"user_email_alias"} && ($#{$entry->{"user_email_alias"}} != -1) ) {
        $ldapEntry->add( mailAlias => $entry->{"user_email_alias"} );
    }

    # L'adresse postale
    if( $entry->{"user_address"} ) {
        # Thunderbird, IceDove... : ne comprennent que cet attribut
        $ldapEntry->add( street => to_utf8({ -string => $entry->{"user_address"}, -charset => $defaultCharSet }) );
        # Outlook : ne comprend que cet attribut
        # Outlook Express : préfère celui-là à 'street'
        $ldapEntry->add( postalAddress => to_utf8({ -string => $entry->{"user_address"}, -charset => $defaultCharSet }) );
    }

    # Le code postal
    if( $entry->{"user_zipcode"} ) {
        $ldapEntry->add( postalCode => to_utf8({ -string => $entry->{"user_zipcode"}, -charset => $defaultCharSet }) );
    }

    # La ville
    if( $entry->{"user_town"} ) {
        $ldapEntry->add( l => to_utf8({ -string => $entry->{"user_town"}, -charset => $defaultCharSet }) );
    }

    # Le domaine
    if( $entry->{"user_domain"} ) {
        $ldapEntry->add( obmDomain => to_utf8({ -string => $entry->{"user_domain"}, -charset => $defaultCharSet }) );
    }

    return 1;
}


sub updateLdapEntry {
    my $self = shift;
    my( $ldapEntry ) = @_;
    my $entry = $self->{"userDesc"};
    my $update = 0;

    # Le champs nom, prenom de l'utilisateur
    my $longName;
    if( $entry->{"user_firstname"} ) {
        $longName = $entry->{"user_firstname"}." ".$entry->{"user_lastname"};
    }else {
        $longName = $entry->{"user_lastname"};
    }

    if( &OBM::Ldap::utils::modifyAttr( $longName, $ldapEntry, "cn" ) ) {
        # On synchronise le nom affichable
        &OBM::Ldap::utils::modifyAttr( $longName, $ldapEntry, "displayName" );

        $update = 1;
    }

    # Le nom de famille
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_lastname"}, $ldapEntry, "sn" ) ) {
        $update = 1;
    }

    # Le prenom
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_firstname"}, $ldapEntry, "givenName" ) ) {
        $update = 1;
    }

    # Le titre
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_title"}, $ldapEntry, "title" ) ) {
        $update = 1;
    }

    # Le service
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_service"}, $ldapEntry, "ou" ) ) {
        $update = 1;
    }

    # La description
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_description"}, $ldapEntry, "description" ) ) {
        $update = 1;
    }

    # L'adresse
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_address"}, $ldapEntry, "street" ) ) {
        &OBM::Ldap::utils::modifyAttr( $entry->{"user_address"}, $ldapEntry, "postalAddress" );
        $update = 1;
    }

    # Le code postal
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_zipcode"}, $ldapEntry, "postalCode" ) ) {
        $update = 1;
    }

    # La ville
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_town"}, $ldapEntry, "l" ) ) {
        $update = 1;
    }

    # Le repertoire personnel
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_homedir"}, $ldapEntry, "homeDirectory" ) ) {
        $update = 1;
    }
            
    # L'UID
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_uid"}, $ldapEntry, "uidNumber" ) ) {
        $update = 1;
    }

    # Le GID
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_gid"}, $ldapEntry, "gidNumber" ) ) {
        $update = 1;
    }

    # Le telephone
    if( &OBM::Ldap::utils::modifyAttrList( $entry->{"user_phone"}, $ldapEntry, "telephoneNumber" ) ) {
        $update = 1;
    }

    # Le fax
    if( &OBM::Ldap::utils::modifyAttrList( $entry->{"user_fax"}, $ldapEntry, "facsimileTelephoneNumber" ) ) {
        $update = 1;
    }

    # Le mobile
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_mobile"}, $ldapEntry, "mobile" ) ) {
        $update = 1;
    }

    # L'acces au web
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_webperms"}, $ldapEntry, "webAccess" ) ) {
        $update = 1;
    }

    # Le domaine
    if( &OBM::Ldap::utils::modifyAttr( $entry->{"user_domain"}, $ldapEntry, "obmDomain") ) {
        $update = 1;
    }

    # L'acces au mail
    if( $entry->{"user_mailperms"} && ( &OBM::Ldap::utils::modifyAttr( "PERMIT", $ldapEntry, "mailAccess" ) ) ) {
        $update = 1;

    }elsif( !$entry->{"user_mailperms"} && ( &OBM::Ldap::utils::modifyAttr( "REJECT", $ldapEntry, "mailAccess" ) ) ) {
        $update = 1;

    }

    # La boite a lettres de l'utilisateur
    if( !$entry->{"user_mailperms"} ) {
        if( &OBM::Ldap::utils::modifyAttr( undef, $ldapEntry, "mailBox" ) ) {
            $update = 1;
        }

    }elsif( &OBM::Ldap::utils::modifyAttr( $entry->{"user_mailbox"}, $ldapEntry, "mailBox" ) ) {
        $update = 1;
    }

    # Le serveur de BAL local
    if( !$entry->{"user_mailperms"} ) {
        if( &OBM::Ldap::utils::modifyAttr( undef, $ldapEntry, "mailBoxServer" ) ) {
            $update = 1;
        }

    }elsif( &OBM::Ldap::utils::modifyAttr( $entry->{"user_mailLocalServer"}, $ldapEntry, "mailBoxServer" ) ) {
        $update = 1;
    }

    # La limitation au domaine local
    if( !$entry->{"user_mailperms"} ) {
        if( &OBM::Ldap::utils::modifyAttr( undef, $ldapEntry, "mailLocalOnly" ) ) {
            $update = 1;
        }

    }elsif( &OBM::Ldap::utils::modifyAttr( $entry->{"user_mailLocalOnly"}, $ldapEntry, "mailLocalOnly" ) ) {
        $update = 1;
    }

    # Le cas des adresses mails
    if( !$entry->{"user_mailperms"} ) {
        # Adresse principales
        if( &OBM::Ldap::utils::modifyAttrList( undef, $ldapEntry, "mail" ) ) {
            $update = 1;
        }

        # Adresses secondaires
        if( &OBM::Ldap::utils::modifyAttrList( undef, $ldapEntry, "mailAlias" ) ) {
            $update = 1;
        }


    }else {
        # Adresse principales
        if( &OBM::Ldap::utils::modifyAttrList( $entry->{"user_email"}, $ldapEntry, "mail" ) ) {
            $update = 1;
        }

        # Adresses secondaires
        if( &OBM::Ldap::utils::modifyAttrList( $entry->{"user_email_alias"}, $ldapEntry, "mailAlias" ) ) {
            $update = 1;
        }
    }

    return $update;
}


sub getMailServerRef {
    my $self = shift;
    my( $domainId, $mailServerId ) = @_;

    if( $self->{"userDesc"}->{"user_mailperms"} ) {
        $$domainId = $self->{"domainId"};
        $$mailServerId = $self->{"userDesc"}->{"user_mailbox_server"};

    }else {
        $$domainId = undef;
        $$mailServerId = undef;

    }

    return 1;
}


sub getMailboxPrefix {
    my $self = shift;
    
    return "user/";
}


sub getMailboxName {
    my $self = shift;
    my $mailBoxName = undef;

    if( $self->{"userDesc"}->{"user_mailperms"} ) {
        $mailBoxName = $self->{"userDesc"}->{"user_mailbox"};
    }

    return $mailBoxName;
}


sub getMailboxQuota {
    my $self = shift;
    my $mailBoxQuota = undef;

    if( $self->{"userDesc"}->{"user_mailperms"} ) {
        $mailBoxQuota = $self->{"userDesc"}->{"user_mailbox_quota"};
    }

    return $mailBoxQuota;
}


sub getMailboxAcl {
    my $self = shift;
    my $mailBoxAcl = undef;

    if( $self->{"userDesc"}->{"user_mailperms"} ) {
        $mailBoxAcl = $self->{"userDesc"}->{"user_mailbox_acl"};
    }

    return $mailBoxAcl;
}


sub getSieveVacation {
    my $self = shift;

    if( !$self->{"userDesc"}->{"user_vacation_enable"} ) {
        return undef;
    }

    if( !$self->{"userDesc"}->{"user_email"} ) {
        return undef;
    }
    my $boxEmails = $self->{"userDesc"}->{"user_email"};
    my $boxEmailsAlias = $self->{"userDesc"}->{"user_email_alias"};

    if( !$self->{"userDesc"}->{"user_vacation_message"} ) {
        return undef;
    }
    my $boxVacationMessage = $self->{"userDesc"}->{"user_vacation_message"};

    my $vacationMsg = "vacation :addresses [ ";
    for( my $i=0; $i<=$#{$boxEmails}; $i++ ) {
        if( $i != 0 ) {
            $vacationMsg .= ", ";
        }
        $vacationMsg .= "\"".$boxEmails->[$i]."\"";
    }

    for( my $i=0; $i<=$#{$boxEmailsAlias}; $i++ ) {
        if( $i != 0 ) {
            $vacationMsg .= ", ";
        }
        $vacationMsg .= "\"".$boxEmailsAlias->[$i]."\"";
    }

    $vacationMsg .= " ] \"".to_utf8( { -string => $boxVacationMessage, -charset => $defaultCharSet } )."\";\n";


    return $vacationMsg;
}


sub getSieveNomade {
    my $self = shift;

    if( !$self->{"userDesc"}->{"user_nomade_perms"} ) {
        return undef;
    }

    if( !$self->{"userDesc"}->{"user_nomade_enable"} ) {
        return undef;
    }

    if( !$self->{"userDesc"}->{"user_nomade_email"} ) {
        return undef;
    }
    my $nomadeEmail = $self->{"userDesc"}->{"user_nomade_email"};

    my $nomadeMsg = "redirect \"".$nomadeEmail."\";\n";

    if( !$self->{"userDesc"}->{"user_nomade_local_copy"} ) {
        $nomadeMsg .= "discard;\n";
        $nomadeMsg .= "stop;\n";
    }else {
        $nomadeMsg .= "keep;\n";
    }

    return $nomadeMsg;
}


sub dump {
    my $self = shift;
    my @desc;

    push( @desc, $self );
    
    require Data::Dumper;
    print Data::Dumper->Dump( \@desc );

    return 1;
}

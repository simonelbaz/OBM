#!/usr/bin/perl -w -T
#+-------------------------------------------------------------------------+
#|   Copyright (c) 1997-2009 OBM.org project members team                  |
#|                                                                         |
#|  This program is free software; you can redistribute it and/or          |
#|  modify it under the terms of the GNU General Public License            |
#|  as published by the Free Software Foundation; version 2                |
#|  of the License.                                                        |
#|                                                                         |
#|  This program is distributed in the hope that it will be useful,        |
#|  but WITHOUT ANY WARRANTY; without even the implied warranty of         |
#|  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          |
#|  GNU General Public License for more details.                           | 
#+-------------------------------------------------------------------------+
#|  http://www.obm.org                                                     |
#+-------------------------------------------------------------------------+


package obmSatellite;

use vars qw(@ISA);
use Net::Server::PreForkSimple;
use URI::Escape;
require ObmSatellite::Parameters::obmSatelliteConf;
use FindBin qw($Bin);
use Unicode::MapUTF8 qw(from_utf8 utf8_supported_charset);
use strict;

@ISA = qw(Net::Server::PreForkSimple);

delete @ENV{qw(IFS CDPATH ENV BASH_ENV PATH)};

obmSatellite->run();
exit;

$|=1;

### Définition des fonctionnalités du service

sub configure_hook {
    my $self = shift;

    # Initalisation des maps Postfix
    if( defined($ObmSatellite::Parameters::obmSatelliteConf::postfixMapsDesc) ) {
        $self->{postfix_maps} = $ObmSatellite::Parameters::obmSatelliteConf::postfixMapsDesc;
    }else {
        print 'Erreur: probleme avec la descriptions des maps Postfix !'."\n";
        exit 1;
    }

    $self->{server}->{name} = 'obmSatellite';                               # Nom du service
    $self->{server}->{conf_file} = '/etc/obm/'.$self->{server}->{name}.'.cf';   # Définition du fichier de configuration du service
    if( ! -r $self->{server}->{conf_file} ) {
        $self->{server}->{conf_file} = $Bin.'/'.$self->{server}->{name}.'.cf';  # Définition du fichier de configuration du service
        if( ! -r $self->{server}->{conf_file} ) {
            print 'Le fichier de configuration \'obm_conf.ini\' n\'existe pas ou n\'est pas lisible'."\n";
            exit 1;
        }
    }


    # Lectrure des paramètres spécifiques
    my $daemonOptions = {
        ldap_server => [],
        ldap_server_tls => [],
        ldap_base => [],
        ldap_login => [],
        ldap_password => [],
        max_badrequest => [],
        log_level => [],
        user => [],
        group => [],
        postmap_cmd => [],
        process_mailbox => [],
        mailbox_map => [],
        process_alias => [],
        alias_map => [],
        process_transport => [],
        transport_map => [],
        process_domain => [],
        domain_map => [],
        process_transport_smtp => [],
        transport_smtp_map => [],
        cyrus_service => [],
        cyrus_imap_conf => [],
        cyrus_partition_root => []
    };
    $self->configure( $daemonOptions );

    # Option chargées depuis le fichier de configuration
    if( defined($daemonOptions->{ldap_server}->[0]) && (($daemonOptions->{ldap_server}->[0] =~ /^((ldap|ldaps):\/\/){0,1}([1-2]?[0-9]{1,2}\.){3}[1-2]?[0-9]{1,2}$/) || ($daemonOptions->{ldap_server}->[0] =~ /^((ldap|ldaps):\/\/){0,1}[a-z0-9-]+(\.[a-z0-9-]+)*\.[a-z]{2,6}$/)) ) {
        $self->{ldap_server}->{server} = $daemonOptions->{ldap_server}->[0];
        if( $self->{ldap_server}->{server} !~ /^(ldap|ldaps):/ ) {
            $self->{ldap_server}->{server} = 'ldap://'.$self->{ldap_server}->{server};
        }

        $self->{ldap_server}->{ldap_server_tls} = $daemonOptions->{ldap_server_tls}->[0];
        if( $self->{ldap_server}->{server} =~ /^ldaps:/ ) {
            $self->{ldap_server}->{ldap_server_tls} = 'none';
        }
        if( !defined($self->{ldap_server}->{ldap_server_tls}) || ($self->{ldap_server}->{ldap_server_tls} !~ /^(none|may|encrypt)$/) ) {
            $self->{ldap_server}->{ldap_server_tls} = 'may';
        }

        $self->{ldap_server}->{base} = $daemonOptions->{ldap_base}->[0];
        if( !defined($self->{ldap_server}->{base}) ) {
            $self->{ldap_server}->{base} = "";
        }

        $self->{ldap_server}->{login} = $daemonOptions->{ldap_login}->[0];
        $self->{ldap_server}->{password} = $daemonOptions->{ldap_password}->[0];
    }else {
        print 'Vous devez renseigner l\'adresse du serveur LDAP !'."\n";
        exit 1;
    }

    if( defined($daemonOptions->{postmap_cmd}->[0]) ) {
        $self->{postmap_cmd} = $daemonOptions->{postmap_cmd}->[0];
    }else {
        print "Vous devez indiquer le chemin d'acces a la commande 'postmap'\n";
        exit 1;
    }

    if( defined($daemonOptions->{max_badrequest}->[0]) && ($daemonOptions->{max_badrequest}->[0] =~ /^\d+$/) ) {
        $self->{server}->{max_badrequest} = $daemonOptions->{max_badrequest}->[0];
    }else {
        $self->{server}->{max_badrequest} = "5";    # Nombre maximal de mauvaises requêtes durant une même session
    }

    if( defined($daemonOptions->{log_level}->[0]) && ($daemonOptions->{log_level}->[0] =~ /^\d$/) ) {
        $self->{server}->{log_level} = $daemonOptions->{log_level}->[0];
    }else {
        $self->{server}->{log_level} = 0;
    }

    # La table des BALs
    if( (defined($daemonOptions->{process_mailbox}->[0])) && ($daemonOptions->{process_mailbox}->[0] =~ /^[01]$/ )) {
        $self->{postfix_maps}->{mailbox}->{postfix_map_process} = $daemonOptions->{process_mailbox}->[0];

        if( $self->{postfix_maps}->{mailbox}->{postfix_map_process} ) {
            if( defined($daemonOptions->{mailbox_map}->[0]) ) {
                $self->{postfix_maps}->{mailbox}->{postfix_map} = $daemonOptions->{mailbox_map}->[0];
            }else {
                $self->{postfix_maps}->{mailbox}->{postfix_map} = "/etc/postfix/virtual_mailbox";
            }
        }
    }else {
        $self->{postfix_maps}->{mailbox}->{postfix_map_process} = 0;
    }

    # La table des alias
    if( (defined($daemonOptions->{process_alias}->[0])) && ($daemonOptions->{process_alias}->[0] =~ /^[01]$/ )) {
        $self->{postfix_maps}->{alias}->{postfix_map_process} = $daemonOptions->{process_alias}->[0];

        if( $self->{postfix_maps}->{alias}->{postfix_map_process} ) {
            if( defined($daemonOptions->{alias_map}->[0]) ) {
                $self->{postfix_maps}->{alias}->{postfix_map} = $daemonOptions->{alias_map}->[0];
            }else {
                $self->{postfix_maps}->{alias}->{postfix_map} = "/etc/postfix/virtual_alias";
            }
        }
    }else {
        $self->{postfix_maps}->{alias}->{postfix_map_process} = 0;
    }

    # La table de transport LMTP
    if( (defined($daemonOptions->{process_transport}->[0])) && ($daemonOptions->{process_transport}->[0] =~ /^[01]$/ )) {
        $self->{postfix_maps}->{transport}->{postfix_map_process} = $daemonOptions->{process_transport}->[0];

        if( $self->{postfix_maps}->{transport}->{postfix_map_process} ) {
            if( defined($daemonOptions->{transport_map}->[0]) ) {
                $self->{postfix_maps}->{transport}->{postfix_map} = $daemonOptions->{transport_map}->[0];
            }else {
                $self->{postfix_maps}->{transport}->{postfix_map} = "/etc/postfix/transport";
            }
        }
    }else {
        $self->{postfix_maps}->{transport}->{postfix_map_process} = 0;
    }

    # La table des domaines
    if( (defined($daemonOptions->{process_domain}->[0])) && ($daemonOptions->{process_domain}->[0] =~ /^[01]$/ )) {
        $self->{postfix_maps}->{domain}->{postfix_map_process} = $daemonOptions->{process_domain}->[0];

        if( $self->{postfix_maps}->{domain}->{postfix_map_process} ) {
            if( defined($daemonOptions->{domain_map}->[0]) ) {
                $self->{postfix_maps}->{domain}->{postfix_map} = $daemonOptions->{domain_map}->[0];
            }else {
                $self->{postfix_maps}->{domain}->{postfix_map} = "/etc/postfix/virtual_domains"
            }
        }
    }else {
        $self->{postfix_maps}->{domain}->{postfix_map_process} = 0;
    }

    # La table de transport SMTP
    if( (defined($daemonOptions->{process_transport_smtp}->[0])) && ($daemonOptions->{process_transport_smtp}->[0] =~ /^[01]$/ )) {
        $self->{postfix_maps}->{transport_smtp}->{postfix_map_process} = $daemonOptions->{process_transport_smtp}->[0];

        if( $self->{postfix_maps}->{transport_smtp}->{postfix_map_process} ) {
            if( defined($daemonOptions->{transport_smtp_map}->[0]) ) {
                $self->{postfix_maps}->{transport_smtp}->{postfix_map} = $daemonOptions->{transport_smtp_map}->[0];
            }else {
                $self->{postfix_maps}->{transport_smtp}->{postfix_map} = "/etc/postfix/transport_smtp";
            }
        }
    }else {
        $self->{postfix_maps}->{transport_smtp}->{postfix_map_process} = 0;
    }

    # Le script du service Cyrus
    if( defined($daemonOptions->{cyrus_service}->[0]) ) {
        $self->{cyrus}->{cyrus_service} = $daemonOptions->{cyrus_service}->[0];
    }else {
        $self->{cyrus}->{cyrus_service} = "/etc/init.d/cyrus";
    }

    # Le fichier de configuration de Cyrus Imapd
    if( defined($daemonOptions->{cyrus_imap_conf}->[0]) ) {
        $self->{cyrus}->{cyrus_imap_conf} = $daemonOptions->{cyrus_imap_conf}->[0];
    }else {
        $self->{cyrus}->{cyrus_imap_conf} = "/etc/imapd.conf";
    }

    # Le chemin d'accès aux partitions Cyrus
    if( defined($daemonOptions->{cyrus_partition_root}->[0]) ) {
        $self->{cyrus}->{cyrus_partition_root} = $daemonOptions->{cyrus_partition_root}->[0];
    }else {
        $self->{cyrus}->{cyrus_partition_root} = "/var/spool/cyrus";
    }


    $self->{server}->{user} = $daemonOptions->{user}->[0];
    $self->{server}->{group} = $daemonOptions->{group}->[0];


    # Lecture des paramètres de configurations standart
    $self->configure;

    # Mise en place des valeurs non modifiables depuis le fichier de
    # configuration
    $self->{server}->{port}  = [ '30000' ];     # Port d'écoute
    $self->{server}->{chdir}  = '/tmp';         # chdir to root

    if( !defined($self->{server}->{user}) ) {
        $self->{server}->{user}   = 'postfix';      # Utilisateur d'exécution du service
    }
    if( !defined($self->{server}->{group}) ) {
        $self->{server}->{group}  = 'postfix';      # Groupe d'exécution du service
    }

    $self->{server}->{setsid} = 1;              # Mode service
    $self->{server}->{no_close_by_child} = 1;   # Le service ne peut-être stoppé par un process fils
    $self->{server}->{max_servers} = 1;         # Nombre maximum de process
    $self->{server}->{max_requests} = 5;        # Nombre maximum de requêtes par process
    $self->{server}->{conn_timeout} = 5;        # Délai entre 2 requêtes
    $self->{server}->{version} = "1.0";         # Serveur version
   

    # Securisation du service
    $self->{server}->{commandline}->[0] = $Bin."/".$self->{server}->{name}.".pl";

    for( my $i=0; $i<=$#{$self->commandline}; $i++ ) {
        my $perlOpt = "^(-w -T)\$";
        if( $self->commandline->[$i] =~ /$perlOpt/ ) {
            $self->commandline->[$i] = $1;
        }

        my $regexpPath = "^(\/[\-_\.\/A-Za-z0-9]*\/".$self->{server}->{name}."\.pl)\$";
        if( $self->commandline->[$i] =~ /$regexpPath/ ) {
            $self->commandline->[$i] = $1;
        }
    }
}


sub pre_loop_hook {
    my $self = shift;

    $self->log( 1, $self->log_time." - Demarrage du service ".$self->{server}->{name}." sur le port ".$self->{server}->{port}->[0] );
}


# Process after a new connection was established end before the requests was
# process
sub post_accept_hook {
    my $self = shift;

    $self->logMessage( "Connexion de : ".$self->{server}->{client}->peerhost()." on port ".$self->{server}->{client}->sockport() );
    $self->sendMessage( "HELLO", undef );
}


# Log refused requests
sub request_denied_hook {
    my $self = shift;

    $self->logMessage( 'Connexion de : '.$self->{'server'}->{'client'}->peerhost().' on port '.$self->{'server'}->{'client'}->sockport().' refusee, les autorisation d\'acces sont elles correctement indiquee dans le fichier de configuration ?' );
}


# Process the request
sub process_request {
    my $self = shift;

    eval {

        # Configuration de l'alarme
        local $SIG{ALRM} = sub {
            $self->sendMessage( 'TIMEOUT', undef );
            die 'timeOut';
        };

        #
        # Nombre courant de mauvaises requêtes
        my $current_badrequest = 0;

        #
        # Mise en place du délai de réception d'une requête
        alarm( $self->{server}->{conn_timeout} );
        while( my $currentRequest = <STDIN> ) {
            # Suppression du délai de réception d'une requête durant son
            # traitement
            alarm(0);
            $self->logMessage( "Requete recue : ".$currentRequest );

            if( !$self->checkClientRequest( \$currentRequest ) ) {
                if( $currentRequest =~ /^quit$/i ) {
                    last;
                }

                if( $currentRequest =~ /^smtpInConf: ([A-Za-z0-9][A-Za-z0-9-]{0,30}[A-Za-z0-9])$/i ) {
                    my $hostName = $1;

                    my $domainList = $self->getServerDomains( "smtp_in", $hostName );
                    if( !defined($domainList) || ( ref($domainList) ne "ARRAY" ) ) {
                        $self->logMessage( "L'hote '".$hostName."' n'est serveur SMTP entrant d'aucun domaine" );
                        $self->sendMessage( "ERROR", $hostName." n'est pas un serveur SMTP entrant" );
                    }else {
                        $self->processPostfixDomains( $domainList );
                    }
                }

                if( $currentRequest =~ /^smtpOutConf: ([A-Za-z0-9][A-Za-z0-9-]{0,30}[A-Za-z0-9])$/i ) {
                    my $hostName = $1;

                    my $domainList = $self->getServerDomains( "smtp_out", $hostName );
                    if( !defined($domainList) || ( ref($domainList) ne "ARRAY" ) ) {
                        $self->logMessage( "L'hote '".$hostName."' n'est serveur SMTP sortant d'aucun domaine" );
                        $self->sendMessage( "ERROR", $hostName." n'est pas un serveur SMTP sortant" );
                    }else {
                        $self->processPostfixDomains( $domainList );
                    }
                }

                if( $currentRequest =~ /^cyrusPartitions: (add|del):([A-Za-z0-9][A-Za-z0-9-]{0,30}[A-Za-z0-9])$/i ) {
                    my $action = $1;
                    my $hostName = $2;

                    my $domainList = $self->getServerDomains( "imap", $hostName );
                    if( !defined($domainList) || ( ref($domainList) ne "ARRAY" ) ) {
                        $self->logMessage( "L'hote '".$hostName."' n'est serveur IMAP d'aucun domaine" );
                        $self->sendMessage( "ERROR", $hostName." n'est pas un serveur IMAP" );
                    }else {
                        $self->processCyrusPartitions( $action, $domainList );
                    }
                }
            }else {
                $self->sendMessage( "BADREQUEST", undef );
                $current_badrequest++;
                if( $current_badrequest >= $self->{server}->{max_badrequest} ) {
                    $self->sendMessage( "BADREQUESTS", undef );
                    die 'badRequest';
                }
            }

            # Re-initialisation du délai de réception d'une requête
            alarm( $self->{server}->{conn_timeout} );
        }

        # Suppression du délai de réception d'une requête
        alarm(0);
    };

    if( $@ =~ /^timeOut/ ) {
        $self->logMessage( "Timed Out" );
        return;
    }elsif( $@ =~ /^badRequest/ ) {
        $self->logMessage( "Trop de mauvaises requetes" );
        return;
    }

}


# Process after all request was process and before ending connection
sub post_process_request_hook {
    my $self = shift;

    $self->sendMessage( 'BYE', undef );

    my $peer = $self->{server}->{client}->peerhost();
    my $logMessage = "Deconnexion";

    if( defined($peer) ) {
        $logMessage .= " de : ".$peer;
    }

    $self->logMessage( $logMessage );
}


sub logMessage {
    my( $self, $msg ) = @_;

    my $peer = $self->{server}->{client}->peerhost();

    if( defined($peer) ) {
        $self->log( 2, $self->log_time." - ".$peer." - ".$msg );
    }else {
        $self->log( 2, $self->log_time." - ".$msg );
    }
}


sub sendMessage {
    my $self = shift;
    my( $msg_code, $msg ) = @_;
    my $cmd = undef;
    
    SWITCH: {
        if( $msg_code !~ /^HELLO|TIMEOUT|BADREQUEST|BADREQUESTS|BYE|OK|ERROR$/ ) {
            $self->logMessage( "Code erreur inconnu : ".$msg_code );
            return 0;
        }

        if( $msg_code =~ /^TIMEOUT$/ ) {
            $cmd = "Timed Out!";
            last SWITCH;
        }

        if( $msg_code =~ /^HELLO$/ ) {
            $cmd = $self->{server}->{name}." v.".$self->{server}->{version};
            last SWITCH;
        }

        if( $msg_code =~ /^BADREQUEST$/ ) {
            $cmd = "Mauvaise requete";
            last SWITCH;
        }

        if( $msg_code =~ /^BADREQUESTS$/ ) {
            $cmd = "Trop de mauvaises requetes !";
            last SWITCH;
        }

        if( $msg_code =~ /^BYE$/ ) {
            $cmd = "Bye !";
            last SWITCH;
        }

        if( $msg_code =~ /^OK$/ ) {
            $cmd = "OK";
            last SWITCH;
        }

        if( $msg_code =~ /^ERROR$/ ) {
            $cmd = "Echec";

            if( defined($msg) ) {
                $cmd .= ": ".$msg;
            }
            last SWITCH;
        }
    }

    if( defined($cmd) ) {
        $self->logMessage( "Requete envoyee : ".$cmd );
        print $cmd."\n";
    }

    return 1;
}


sub checkClientRequest {
    my $self = shift;
    my( $request ) = @_;

    $/="\r\n";
    chomp($$request);
    SWITCH: {
        if( $$request =~ /^quit$/i ) {
            return 0;
        }
        
        if( $$request =~ /^smtpInConf: (.+:)*.+$/i ) {
            return 0;
        }
        
        if( $$request =~ /^smtpOutConf: (.+:)*.+$/i ) {
            return 0;
        }
        
        if( $$request =~ /^cyrusPartitions: (.+:)*.+$/i ) {
            return 0;
        }
    }

    return 1;

}


sub getServerDomains {
    my $self = shift;
    my( $type, $hostName ) = @_;
    my $domainList;

    if( !defined($type) || ($type =~ /^$/) ) {
        return $domainList;
    }

    if( !defined($hostName) || ($hostName =~ /^$/) ) {
        return $domainList;
    }


    my $ldapFilter;

    SWITCH: {
        if( $type =~ /^smtp_in$/ ) {
            $self->logMessage( "Obtention des domaines du serveur '".$hostName."' de type SMTP-in" );
            $ldapFilter = "smtpInHost=".$hostName;
            last SWITCH;
        }

        if( $type =~ /^smtp_out$/ ) {
            $self->logMessage( "Obtention des domaines du serveur '".$hostName."' de type SMTP-out" );
            $ldapFilter = "smtpOutHost=".$hostName;
            last SWITCH;
        }

        if( $type =~ /^imap$/ ) {
            $self->logMessage( "Obtention des domaines du serveur '".$hostName."' de type IMAP" );
            $ldapFilter = "imapHost=".$hostName;
            last SWITCH;
        }

        # Type inconnu
        $self->logMessage( "Type de serveur inconnu '".$type."'" );
        return $domainList;
    }

    $ldapFilter = "(&(objectclass=obmMailServer)(".$ldapFilter."))";
    my $ldapAttributes = [ 'obmDomain' ];


    # LDAP connexion
    if( defined($self->{"ldap_server"}->{"login"}) ) {
        $self->logMessage( "Connexion authentifiee a l'annuaire LDAP" );
    }else {
        $self->logMessage( "Connexion anonyme a l'annuaire LDAP" );
    }

    if( !&ObmSatellite::utils::connectLdapSrv( $self->{ldap_server} ) ) {
        $self->logMessage( "Echec: connexion a l'annuaire LDAP" );
        print "Echec: Impossible de se connecter a l'annuaire LDAP\n";
        return 1;
    }

    my @ldapEntries;
    if( &ObmSatellite::utils::ldapSearch( $self->{ldap_server}, \@ldapEntries, $ldapFilter, $ldapAttributes ) ) {
        $self->logMessage( "Echec: lors de l'obtention des informations de l'hote '".$hostName."'" );
        return 1;
    }

    for( my $i=0; $i<=$#ldapEntries; $i++ ) {
        my $entryDomainList = $ldapEntries[$i]->get_value( $ldapAttributes->[0], asref => 1 );
        for( my $j=0; $j<=$#{$entryDomainList}; $j++ ) {
            $entryDomainList->[$j] = from_utf8( { -string => $entryDomainList->[$j], -charset => "ISO-8859-1" } );
            push( @{$domainList}, $entryDomainList->[$j] );
        }
    }

    if( $#{$domainList} < 0 ) {
        $self->logMessage( "Pas de domaines associes a l'hote '".$hostName."'" );
    }else {
        $self->logMessage( "Domaines associes a l'hote '".$hostName."' : ".join( ",", @{$domainList} ) );
    }

    $self->logMessage( "Deconnexion de l'annuaire LDAP" );
    &ObmSatellite::utils::disconnectLdapSrv( $self->{ldap_server} );

    return $domainList;
}


sub processPostfixDomains {
    my $self = shift;
    my ( $domainList ) = @_;
    use ObmSatellite::utils;

    my $errors = 0;

    # LDAP connection
    if( defined($self->{"ldap_server"}->{"login"}) ) {
        $self->logMessage( "Connexion authentifiee a l'annuaire LDAP" );
    }else {
        $self->logMessage( "Connexion anonyme a l'annuaire LDAP" );
    }

    if( !&ObmSatellite::utils::connectLdapSrv( $self->{ldap_server} ) ) {
        $self->logMessage( "Echec: connexion a l'annuaire LDAP" );
        print "Echec: Impossible de se connecter a l'annuaire LDAP\n";
        return 1;
    }

    foreach my $map ( keys(%{$self->{postfix_maps}}) ) {
        if( ($self->{postfix_maps}->{$map}->{postfix_map_process}) && defined($self->{postfix_maps}->{$map}->{make_map}) ) {
            $self->logMessage( "Traitement de la map de type : '".$map."'" );

            if( &{$self->{postfix_maps}->{$map}->{make_map}}( $self, $self->{postfix_maps}->{$map}, $domainList ) ) {
                $self->logMessage( "Echec lors de la creation du fichier plat de la map : '".$map."'" );

                $errors = 1;
                last;
            }
        }else {
            $self->{postfix_maps}->{$map}->{postfix_map_postmap} = 0;
        }
    }

    $self->logMessage( "Deconnexion de l'annuaire LDAP" );
    &ObmSatellite::utils::disconnectLdapSrv( $self->{ldap_server} );

    if( !$errors ) {
        my $postmapCmd = $self->{postmap_cmd};
        $self->logMessage( "Generation des maps" );

        foreach my $map ( keys(%{$self->{postfix_maps}}) ) {
            if( $self->{postfix_maps}->{$map}->{postfix_map_postmap} ) {
                my $mapName = $self->{postfix_maps}->{$map}->{postfix_map};
                my $mapType = $self->{postfix_maps}->{$map}->{postfix_map_type};

                $self->logMessage( "Generation de la map '".$map."' de type '".$mapType."', fichier '".$mapName."'" );

                my $cmd = $self->{postmap_cmd}." ".$mapType.":".$mapName;
                $self->logMessage( "Execution de : '".$cmd."'" );
                my $ret = 0xffff & system $cmd;

                if( $ret ) {
                    $errors = 2;
                    last;
                }

            }
        }
    }else {
        $self->logMessage( "Generation des maps annulee" );
    }

    if( $errors == 0 ) {
        $self->sendMessage( "OK", undef );
    }elsif( $errors == 1 ) {
        $self->sendMessage( "ERROR", "lors de la creation des fichiers plats" );
    }elsif( $errors == 2 ) {
        $self->sendMessage( "ERROR", "lors de la generation des map Postfix !" );
    }else {
        $self->sendMessage( "ERROR", "erreur inconnue !" );
    }

    $self->logMessage( "Fin du traitement" );

    return 0;
}


sub processCyrusPartitions {
    my $self = shift;
    my ( $action, $domains ) = @_;

    my $errors = 0;

    require ObmSatellite::cyrusPartitions;
    my $cyrusPartitions = ObmSatellite::cyrusPartitions->new( $self, $domains );

    if( !defined($cyrusPartitions) ) {
        $self->logMessage( 'Echec: lors de la creation de l\'objet \'cyrusPartitions\'' );
        $errors = 1;
    }

    if( !$errors ) {
        SWITCH: {
            if( $action eq 'add' ) {
                if( $cyrusPartitions->addPartitions() ) {
                    $self->logMessage( 'Echec: lors du traitement du fichier de configuration de Cyrus' );
                    $errors = 2;
                }
                last SWITCH;
            }

            if( $action eq 'del' ) {
                last SWITCH;
            }
        }
    }

    if( !$errors ) {
        # Re-démarrage du service Cyrus Imapd
        my $cmd = $self->{cyrus}->{cyrus_service}.' stop > /dev/null 2>&1';
        $self->logMessage( 'Execution de : \''.$cmd.'\'' );
        my $ret = 0xffff & system $cmd;
    
        if( $ret ) {
            $self->logMessage( 'Echec: lors de l\'arret du service Cyrus' );
            $errors = 3;
        }
    }
    
    if( !$errors ) {
        my $cmd = $self->{cyrus}->{cyrus_service}.' start > /dev/null 2>&1';
        $self->logMessage( 'Execution de : \''.$cmd.'\'' );
        my $ret = 0xffff & system $cmd;
    
        if( $ret ) {
            $self->logMessage( "Echec: lors du redemrrage du service Cyrus" );
            $errors = 4;
        }
    }

    if( $errors == 0 ) {
        # On attend un temps pour s'assurer que le service est correctement
        # accessible
        $self->sendMessage( "OK", undef );
    }elsif( $errors == 1 ) {
        $self->sendMessage( "ERROR", "mise a jour des partitions Cyrus impossible !" );
    }elsif( $errors == 2 ) {
        $self->sendMessage( "ERROR", "probleme lors du traitement du fichier de configuration de Cyrus" );
    }elsif( $errors == 3 ) {
        $self->sendMessage( "ERROR", "probleme lors de l'arret du service Cyrus Imapd" );
    }elsif( $errors == 4 ) {
        $self->sendMessage( "ERROR", "probleme lors du redemarrage du service Cyrus Imapd" );
    }else {
        $self->sendMessage( "ERROR", "erreur inconnue !" );
    }

    $self->logMessage( "Fin du traitement" );
    return 0;
}

__END__

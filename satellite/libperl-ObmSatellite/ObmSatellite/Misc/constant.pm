#########################################################################
# OBM       - file : ObmSatellite::Misc::regex (Perl Module)            #
#           - Desc : common regexp used on OBM                          #
#########################################################################
package ObmSatellite::Misc::constant;

require Exporter;


@ISA = qw(Exporter);
@EXPORT_constant = qw(
    IMAPD_CONF_FILE
    TMP_DIR
    OBM_TAR_COMMAND
    OBM_GLOBAL_DOMAIN_NAME
    );
@EXPORT = (@EXPORT_constant);

use constant IMAPD_CONF_FILE => '/etc/imapd.conf';

use constant TMP_DIR => '/var/tmp';

use constant OBM_TAR_COMMAND => '/bin/tar';

use constant OBM_GLOBAL_DOMAIN_NAME => 'global.virt';

package vat.core.implement;

import vat.api.Disposable;
import vat.api.implement.Web;

///
/// @author Zen.Liu
/// @since 2025-12-10
public interface Components extends Disposable {
    /// An endpoint component provide to register sections of endpoints
    /// The implements should have a contractor of (DomainManager dm,JsonObject conf,Logger log)
    interface Endpoint extends Components {
        void routing(Web web);
    }
}

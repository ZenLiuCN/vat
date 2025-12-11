package vat.api;

///
/// Marker of domain objects. each one will generate a domain-identifier from its canonical name.
///
/// @author Zen.Liu
/// @since 2025-10-20
///
@Prototype
public interface Domain {
    Class<? extends Domain> domainIdentity();

    /// context marker
    interface Context {
    }
}
